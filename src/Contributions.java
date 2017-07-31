//
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataRead;


public class Contributions{
	//
	public static void main(String[] args){
		try{
			DataDescriptor dd=DiagnosisFactory.getDataDescriptor("d:/Data/DiagnosisVortex/Haima/Haima_model.ctl");
			
			Range r=new Range("lev(375,175);y(5,40)",dd);
			
			Variable vs=new Variable("vs" ,r);
			Variable v1=new Variable("vs1",r);
			Variable v4=new Variable("vs4",r);
			Variable v5=new Variable("vs5",r);
			Variable v6=new Variable("vs6",r);
			Variable v7=new Variable("vs7",r);
			
			DataRead dr=DataIOFactory.getDataRead(dd);
			dr.readData(vs,v1,v4,v5,v6,v7);	dr.closeFile();
			
			Variable vsc=vs.copy();
			
			Variable vsm=vs.multiply(vs ).anomalizeZY();
			Variable vs1=v1.multiply(vsc).anomalizeZY().divide(vsm);
			Variable vs4=v4.multiply(vsc).anomalizeZY().divide(vsm);
			Variable vs5=v5.multiply(vsc).anomalizeZY().divide(vsm);
			Variable vs6=v6.multiply(vsc).anomalizeZY().divide(vsm);
			Variable vs7=v7.multiply(vsc).anomalizeZY().divide(vsm);
			
			for(int i=0;i<vs.getTCount();i++)
			System.out.println(
				vs1.getData()[0][0][0][i]+","+
				vs4.getData()[0][0][0][i]+","+
				vs5.getData()[0][0][0][i]+","+
				vs6.getData()[0][0][0][i]+","+
				vs7.getData()[0][0][0][i]
			);
			
	    }catch(Exception ex){ ex.printStackTrace();}
	}
}
