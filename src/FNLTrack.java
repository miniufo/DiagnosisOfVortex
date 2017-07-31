//
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.MDate;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.Variable;


//
public final class FNLTrack{
	//
	static final  String name ="Bart";
	static final  String year ="1999";
	static final  String ipath="//lynn/Data/GRIB/FNL/"+year+"/";
	static final  String opath="d:/Data/DiagnosisVortex/"+name+"/";
	static final boolean grib2=false;
	
	static   int[] xpos=null;
	static   int[] ypos=null;
	static float[] minv=null;
	
	//
	public static void main(String[] args){
		getSLPData("09","19","00",37);
		
		DiagnosisFactory df=DiagnosisFactory.parseFile("d:/Data/DiagnosisVortex/"+name+"/slp.ctl");
		DataDescriptor dd=df.getDataDescriptor();
		
		Variable p=df.getVariables(new Range("z(1,1)",dd),"slp")[0];
		
		Variable[] vs=DiagnosisFactory.getVariables(
			"d:/Data/DiagnosisVortex/"+name+"/"+name+".ctl","z(1,1)","u10m","v10m"
		);
		
		Variable w=vs[0].hypotenuse(vs[1]);
		
		getMin(p,129,111,5);
		
		  int[] minxpos=xpos.clone();
		  int[] minypos=ypos.clone();
		float[] minval =minv.clone();
		
		getMax(w,2,minxpos,minypos);
		
		  int[] maxxpos=xpos.clone();
		  int[] maxypos=ypos.clone();
		float[] maxval =minv.clone();
		
		for(int l=0;l<p.getTCount();l++)
		System.out.println(
			(l+1)+"\t"+
			minxpos[l]+" "+(minypos[l]-90)+" "+minval[l]/100+"\t"+
			maxxpos[l]+" "+(maxypos[l]-90)+" "+maxval[l]
		);
	}
	
	static void writeSLPCtl(MDate tstr,int tcount){
		StringBuffer sb=new StringBuffer();
		
		sb.append("dset ^slp.dat\n");
		if(!grib2) sb.append("options yrev\n");
		sb.append("undef 9.999E+20\n");
		sb.append("title grib data of "+name+"\n");
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
}
