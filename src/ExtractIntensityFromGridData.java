//
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.Variable;
import miniufo.io.CtlDataWriteStream;


//
public final class ExtractIntensityFromGridData{
	//
	static   int[] xpos=null;
	static   int[] ypos=null;
	static float[] minv=null;
	
	//
	public static void main(String[] args){
		String path="d:/Data/Megi/New/";
		
		Variable slpCtl=getMinSLP(path+"slp_ctrl.ctl");
		Variable slpsn1=getMinSLP(path+"slp_sen0.8.ctl");
		Variable slpsn2=getMinSLP(path+"slp_sen1.0.ctl");
		Variable slpsn3=getMinSLP(path+"slp_sen1.3.ctl");
		Variable slpsn4=getMinSLP(path+"slp_sen1.5.ctl");
		
		slpCtl.setName("slpctl");
		slpsn1.setName("slpsn0.8");
		slpsn2.setName("slpsn1.0");
		slpsn3.setName("slpsn1.3");
		slpsn4.setName("slpsn1.5");
		
		CtlDataWriteStream cdws=new CtlDataWriteStream(path+"slp.dat");
		cdws.writeData(slpCtl,slpsn1,slpsn2,slpsn3,slpsn4);	cdws.closeFile();
	}
	
	static Variable getMinSLP(String fname){
		DiagnosisFactory df=DiagnosisFactory.parseFile(fname);
		DataDescriptor dd=df.getDataDescriptor();
		
		Variable p=df.getVariables(new Range("z(1,1)",dd),"slp")[0];
		
		getMin(p,300,170,70);
		
		Variable slp=new Variable("slp",false,new Range(dd.getTCount(),1,1,1));
		slp.setCommentAndUnit("");
		
		slp.getData()[0][0][0]=minv.clone();
		
		for(int l=0;l<p.getTCount();l++)
		System.out.println(
			(l+1)+"\t"+
			xpos[l]+" "+(ypos[l]-90)+" "+minv[l]
		);
		System.out.println();
		
		return slp;
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
}
