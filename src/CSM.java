//
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import miniufo.database.AccessBestTrack;
import miniufo.database.AccessBestTrack.DataSets;
import miniufo.descriptor.CsmDescriptor;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.MDate;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;
import miniufo.lagrangian.Typhoon;

//
public class CSM{
	//
	static final int count=38;
	
	static final boolean grib2=false;
	static final boolean hasFNLData=true;
	
	static final String year="2006";
	static final String mnth="09";
	static final String day ="10";
	static final String hour="00";
	static final String TCName="Shanshan";
	static final String ipath="//lynn/Data/GRIB/FNL/"+year+"/";
	static final String opath="d:/Data/DiagnosisVortex/"+TCName+"/";
	static final String ranges="time=1Jan"+year+"-31Dec"+year+";name="+TCName;
	
	static final long str=new MDate(
		Integer.parseInt(year),
		Integer.parseInt(mnth),
		Integer.parseInt(day ),
		Integer.parseInt(hour),0
	).getLongTime();
	
	static final DataSets dsets=DataSets.JMA;
	
	static   int[] xpos=null;
	static   int[] ypos=null;
	static float[] minv=null;
	
	
	//
	public static void main(String[] args){
		/******************************************************************/
		/**                     generating CSM file                      **/
		/******************************************************************/
		List<Typhoon> ls=getRecords(dsets,ranges);
		
		if(ls.size()!=1) throw new IllegalArgumentException("found too many TCs");
		
		Typhoon tr=ls.get(0);
		
		try(FileWriter fw=new FileWriter(new File(opath+TCName+".csm"))){
			fw.write(toCSMString(tr,
				tr.getXPositions(),
				tr.getYPositions(),
				tr.getAttachedData(Typhoon.Pmin),
				tr.getAttachedData(Typhoon.Vmax))
			);
			
		}catch(Exception e){ e.printStackTrace();System.exit(0);}
		
		
		
		/******************************************************************/
		/**                  generating _FNL.CSM file                    **/
		/******************************************************************/
		getSLPData(mnth,day,hour,count);
		
		DiagnosisFactory df=DiagnosisFactory.parseFile(opath+"slp.ctl");
		DataDescriptor dd=df.getDataDescriptor();
		
		Variable p=df.getVariables(new Range("z(1,1)",dd),"slp")[0];
		
		int[] pos=getInitPoint(tr);
		
		getMin(p,pos[0],pos[1],5);
		
		  int[] minxpos=xpos.clone();
		  int[] minypos=ypos.clone();
		float[] minval =minv.clone();
		
		for(int l=0;l<p.getTCount();l++){
			minypos[l]-=90;
			minval[l]/=100f;
		}
		
		try{
			FileWriter fw=new FileWriter(new File(opath+TCName+"_FNL.csm"));
			fw.write(toCSMString(tr,minxpos,minypos,minval));	fw.close();
			
		}catch(Exception e){ e.printStackTrace();System.exit(0);}
		
		
		
		/******************************************************************/
		/**               generating GrADS Intensity file                **/
		/******************************************************************/
		CsmDescriptor csmBST=new CsmDescriptor(new File(opath+TCName+".csm"));
		
		CsmDescriptor csmFNL=null;
		if(hasFNLData) csmFNL=new CsmDescriptor(new File(opath+TCName+"_FNL.csm"));
		
		Range rBST=new Range("x(1,1);y(1,1);z(1,1)",csmBST);
		Range rFNL=new Range("x(1,1);y(1,1);z(1,1)",csmFNL);
		
		Variable wnd=new Variable("wnd",false,rBST);	wnd.setUndef(-9999);
		Variable prs=new Variable("prs",false,rBST);	prs.setUndef(-9999);
		Variable slp=null;
		
		if(hasFNLData){
			slp=new Variable("slp",false,rFNL);	slp.setUndef(-9999);
		}
		
		float[][][][] wdata=wnd.getData();
		float[][][][] pdata=prs.getData();
		
		float[][][][] sdata=null;
		if(hasFNLData){
			sdata=slp.getData();
		}
		
		System.arraycopy(csmBST.getMaxWind(),0,wdata[0][0][0],0,csmBST.getTCount());
		System.arraycopy(csmBST.getMinPres(),0,pdata[0][0][0],0,csmBST.getTCount());
		
		if(hasFNLData){
			System.arraycopy(csmFNL.getMinPres(),0,sdata[0][0][0],0,csmFNL.getTCount());
		}
		
		DataWrite dw=DataIOFactory.getDataWrite(
			csmBST.getCtlDescriptor(),opath+TCName+"_CSM.dat"
		);
		
		if(hasFNLData) dw.writeData(csmBST,wnd,prs,slp);
		else dw.writeData(csmBST,wnd,prs);
		
		dw.closeFile();
	}
	
	static int[] getInitPoint(Typhoon tr){
		int tstart=getTOffset(tr);
		
		int[] pos=new int[2];
		
		pos[0]=(int)tr.getXPositions()[tstart];
		pos[1]=(int)(tr.getYPositions()[tstart]+90);
		
		return pos;
	}
	
	static int getTOffset(Typhoon tr){
		int tstart=0;
		
		long[] ts=tr.getTimes();
		
		for(int l=0;l<ts.length;l++) if(ts[l]==str){ tstart=l; break;}
		
		return tstart;
	}
	
	static String toCSMString(Typhoon tr,float[] lons,float[] lats,float[] prs,float[] wnd){
		int tstart=getTOffset(tr);
		
		StringBuilder sb=new StringBuilder();
		
		sb.append("dpath ^"+TCName+".ctl\n");
		sb.append("title "+tr.getName()+" ("+tr.getID()+") \n");
		sb.append("xdef 72\n");
		sb.append("ydef 50 0.3\n");
		sb.append("zdef 37 1000 -25\n");
		sb.append(
			"tdef "+count+" "+new MDate(str).toGradsDate()+
			" 6hr\n"
		);
		sb.append("coords\n");
		for(int l=0;l<count;l++)
		sb.append(lons[tstart+l]+" "+lats[tstart+l]+" "+prs[tstart+l]+" "+wnd[tstart+l]+"\n");
		sb.append("endcoords\n");
		
		return sb.toString();
	}
	
	static String toCSMString(Typhoon tr,int[] lons,int[] lats,float[] slp){
		long str=new MDate(
			Integer.parseInt(year),
			Integer.parseInt(mnth),
			Integer.parseInt(day ),
			Integer.parseInt(hour),0
		).getLongTime();
		
		int tstart=0;
		
		if(count!=lons.length){
			long[] ts=tr.getTimes();
			
			for(int l=0;l<ts.length;l++)
			if(ts[l]==str){ tstart=l; break;}
			System.out.println(tstart);
		}
		
		StringBuilder sb=new StringBuilder();
		
		sb.append("dpath ^"+TCName+".ctl\n");
		sb.append("title "+tr.getName()+" ("+tr.getID()+") \n");
		sb.append("xdef 72\n");
		sb.append("ydef 50 0.3\n");
		sb.append("zdef 37 1000 -25\n");
		sb.append(
			"tdef "+count+" "+new MDate(str).toGradsDate()+
			" 6hr\n"
		);
		sb.append("coords\n");
		for(int l=0;l<count;l++)
		sb.append(lons[tstart+l]+" "+lats[tstart+l]+" "+slp[tstart+l]+"\n");
		sb.append("endcoords\n");
		
		return sb.toString();
	}	
	
	static void writeSLPCtl(MDate tstr,int tcount){
		StringBuffer sb=new StringBuffer();
		
		sb.append("dset ^slp.dat\n");
		if(!grib2) sb.append("options yrev\n");
		sb.append("undef 9.999E+20\n");
		sb.append("title FNL data of "+TCName+"\n");
		sb.append("xdef  360 linear    0    1\n");
		sb.append("ydef  181 linear  -90    1\n");
		sb.append("tdef   "+tcount+" linear "+tstr.toGradsDate()+" 6hr\n");
		sb.append("zdef    1 levels 1000\n");
		sb.append("vars 1\n");
		sb.append("slp 0    99 sea level pressure (Pa)\n");
		sb.append("endvars\n");
		
		try{
			FileWriter fw=new FileWriter(new File(opath+"slp.ctl"));
			fw.write(sb.toString());	fw.close();
			
		}catch(Exception e){ e.printStackTrace(); System.exit(0);}
	}
	
	static void getSLPData(String mon,String day,String hr,int tcount){
		File f=new File(opath+"slp.dat");
		if(f.exists()) f.delete();
		
		MDate md=new MDate(
			Integer.parseInt(year),Integer.parseInt(mon),
			Integer.parseInt(day ),Integer.parseInt(hr ),0
		);
		
		MDate tmp=md.copy();
		
		for(int i=0;i<tcount;i++){
			String m=String.format("%1$02d",tmp.getMonth());
			String d=String.format("%1$02d",tmp.getDate());
			String h=String.format("%1$02d",tmp.getHour());
			
			String fname="fnl_"+year+m+d+"_"+h+"_00";
			
			int idnum=getSLPIDNum(fname);
			
			try{
				Process p=null;
				
				if(grib2) p=Runtime.getRuntime().exec(
					"wgrib2 -v -d "+idnum+" "+ipath+fname+" -no_header -append -bin "+opath+"slp.dat"
				);
				else p=Runtime.getRuntime().exec(
					"wgrib -v -d "+idnum+" "+ipath+fname+" -nh -bin -append -o "+opath+"slp.dat"
				);
				
				BufferedReader br=new BufferedReader(new InputStreamReader(p.getInputStream()));
				String str=br.readLine();
				
				while(str!=null){
					if(str.indexOf("P")!=-1) System.out.println(str);
					
					str=br.readLine();
				}
				
				br.close();	p.destroy();
				
			}catch(IOException e){ e.printStackTrace(); System.exit(0);}
			
			tmp=tmp.add("6hr");
		}
		
		writeSLPCtl(md,tcount);
	}
	
	static int getSLPIDNum(String fname){
		int idnum=0;
		
		try{
			Process p=null;
			if(grib2) p=Runtime.getRuntime().exec("wgrib2 -s "+ipath+"/"+fname);
			else p=Runtime.getRuntime().exec("wgrib -s "+ipath+"/"+fname);
			
			BufferedReader br=new BufferedReader(new InputStreamReader(p.getInputStream()));
			String str=br.readLine();
			
			while(str!=null){
				if(str.indexOf(":PRMSL:")!=-1){ idnum=Integer.parseInt(str.substring(0,3)); break;}
				
				str=br.readLine();
			}
			
			br.close();	p.destroy();
			
		}catch(IOException e){ e.printStackTrace(); System.exit(0);}
		
		return idnum;
	}
	
	static void getMin(Variable v,int oX,int oY,int rad){
		int t=v.getTCount();
		
		int cX=oX,cY=oY;
		
		xpos=new int[t];
		ypos=new int[t];
		minv=new float[t];
		
		float[][][][] vdata=v.getData();
		
		for(int l=0;l<t;l++){
			minv[l]=Float.MAX_VALUE;
			
			for(int j=cY-rad,J=cY+rad;j<=J;j++)
			for(int i=cX-rad,I=cX+rad;i<=I;i++){
				if(vdata[0][j][i][l]<minv[l]){
					minv[l]=vdata[0][j][i][l];
					xpos[l]=i;
					ypos[l]=j;
				}
			}
			
			cX=xpos[l];
			cY=ypos[l];
		}
	}
	
	static void getMax(Variable v,int rad,int[] xref,int[] yref){
		int t=v.getTCount();
		
		xpos=new int[t];
		ypos=new int[t];
		minv=new float[t];
		
		float[][][][] vdata=v.getData();
		
		for(int l=0;l<t;l++){
			minv[l]=Float.MIN_VALUE;
			
			for(int j=yref[l]-rad,J=yref[l]+rad;j<=J;j++)
			for(int i=xref[l]-rad,I=xref[l]+rad;i<=I;i++)
			if(vdata[0][j][i][l]>minv[l]){
				minv[l]=vdata[0][j][i][l];
				xpos[l]=i;
				ypos[l]=j;
			}
		}
	}
	
	public static List<Typhoon> getRecords(DataSets ds,String range){
		final String CMAPath ="D:/Data/Typhoons/CMA/CMA.txt";
		final String JMAPath ="D:/Data/Typhoons/JMA/JMA.txt";
		final String JTWCPath="D:/Data/Typhoons/JTWC/JTWC.txt";
		
		List<Typhoon> ls=null;
		
		switch(ds){
		case CMA:
			ls=AccessBestTrack.getTyphoons(CMAPath,range,ds);
			break;
			
		case JMA:
			ls=AccessBestTrack.getTyphoons(JMAPath,range,ds);
			break;
			
		case JTWC:
			ls=AccessBestTrack.getTyphoons(JTWCPath,range,ds);
			break;
			
		default:
			throw new IllegalArgumentException("unsupported DataSet");
		}
		
		int count=0;
		for(Typhoon tr:ls) count+=tr.getTCount();
		
		System.out.println("total TCs ("+ds+"):"+ls.size()+" ("+count+" samples)");
		
		return ls;
	}
}
