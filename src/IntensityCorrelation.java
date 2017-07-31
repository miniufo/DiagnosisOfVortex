//
import miniufo.application.statisticsModel.FilterMethods;
import miniufo.application.statisticsModel.StatisticsBasicAnalysisMethods;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;


public class IntensityCorrelation{
	//
	public static void main(String[] args){
		try{
			String name="Haima";
			
			DiagnosisFactory df=DiagnosisFactory.parseFile("d:/Data/DiagnosisVortex/"+name+"/"+name+"_model.ctl");
			DataDescriptor dd=df.getDataDescriptor();
			Variable[] vel=df.getVariables(new Range("",dd),false,"vr","w");
			
			df=DiagnosisFactory.parseFile("d:/Data/DiagnosisVortex/"+name+"/"+name+"_CSM.ctl");
			dd=df.getDataDescriptor();
			Variable[] prs=df.getVariables(new Range("",dd),false,"prs");
			
			Variable co_vr0=StatisticsBasicAnalysisMethods.taco(prs[0],vel[0]);co_vr0.setName("tvr0");
			Variable co_w0 =StatisticsBasicAnalysisMethods.taco(prs[0],vel[1]);co_w0.setName("tw0");
			
			FilterMethods.TRunningMean(vel[0],5);
			FilterMethods.TRunningMean(vel[1],5);
			
			Variable co_vr1=StatisticsBasicAnalysisMethods.taco(prs[0],vel[0]);co_vr1.setName("tvr1");
			Variable co_w1 =StatisticsBasicAnalysisMethods.taco(prs[0],vel[1]);co_w1.setName("tw1");
			
			DataWrite dw=DataIOFactory.getDataWrite(dd,"d:/Data/DiagnosisVortex/"+name+"/"+name+"_corr.dat");
			dw.writeData(dd,co_vr0,co_w0,co_vr1,co_w1);	dw.closeFile();
			
	    }catch(Exception ex){ ex.printStackTrace();}
	}
}
