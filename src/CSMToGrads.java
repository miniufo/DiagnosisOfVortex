//
import java.io.File;
import miniufo.descriptor.CsmDescriptor;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;


public class CSMToGrads{
	//
	public static void main(String[] args){
		try{
			String name="Bart";
			boolean hasFNLData=true;
			
			CsmDescriptor csmBST=new CsmDescriptor(new File("D:/Data/DiagnosisVortex/"+name+"/"+name+".csm"));
			
			CsmDescriptor csmFNL=null;
			if(hasFNLData) csmFNL=new CsmDescriptor(new File("D:/Data/DiagnosisVortex/"+name+"/"+name+"_FNL.csm"));
			
			Range rBST=new Range("x(1,1);y(1,1);z(1,1)",csmBST);
			Range rFNL=new Range("x(1,1);y(1,1);z(1,1)",csmFNL);
			
			Variable wnd=new Variable("wnd",false,rBST);	wnd.setUndef(-9999);
			Variable prs=new Variable("prs",false,rBST);	prs.setUndef(-9999);
			Variable slp=null;
			Variable mxw=null;
			
			if(hasFNLData){
				slp=new Variable("slp",false,rFNL);	slp.setUndef(-9999);
				mxw=new Variable("mxw",false,rFNL);	mxw.setUndef(-9999);
			}
			
			float[][][][] wdata=wnd.getData();
			float[][][][] pdata=prs.getData();
			
			float[][][][] sdata=null;
			float[][][][] mdata=null;
			if(hasFNLData){
				sdata=slp.getData();
				mdata=mxw.getData();
			}
			
			System.arraycopy(csmBST.getMaxWind(),0,wdata[0][0][0],0,csmBST.getTCount());
			System.arraycopy(csmBST.getMinPres(),0,pdata[0][0][0],0,csmBST.getTCount());
			
			if(hasFNLData){
				System.arraycopy(csmFNL.getMinPres(),0,sdata[0][0][0],0,csmFNL.getTCount());
				System.arraycopy(csmFNL.getMaxWind(),0,mdata[0][0][0],0,csmFNL.getTCount());
			}
			
			DataWrite dw=DataIOFactory.getDataWrite(
				csmBST.getCtlDescriptor(),
				"d:/Data/DiagnosisVortex/"+name+"/"+name+"_CSM.dat"
			);
			
			if(hasFNLData) dw.writeData(csmBST,wnd,prs,slp,mxw);
			else dw.writeData(csmBST,wnd,prs);
			
			dw.closeFile();
			
	    }catch(Exception ex){ ex.printStackTrace();}
	}
}
