//
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.Variable;
import static miniufo.statistics.StatisticsUtil.cCorrelationCoefficient;


public class IndexCorrelation{
	//
	public static void main(String[] args){
		String name="Lupit";
		
		DiagnosisFactory df=DiagnosisFactory.parseFile("d:/Data/DiagnosisVortex/"+name+"/"+name+"_model.ctl");
		DataDescriptor dd=df.getDataDescriptor();
		Variable dh=df.getVariables(new Range("y(1,22);lev(975,100)",dd),false,"dhFF")[0];
		Variable ea=df.getVariables(new Range("y(19,37);lev(350,175)",dd),false,"edyadvFF")[0];
		Variable fr=df.getVariables(new Range("y(1,25);lev(975,850)",dd),false,"frictFF")[0];
		dh=dh.anomalizeZY();	dh.multiplyEq(2.6e8f*-9f/160f);
		ea=ea.anomalizeZY();	ea.multiplyEq(-2.6e8f);
		fr=fr.anomalizeZY();	fr.multiplyEq(2.6e8f);
		Variable idx=dh.plus(ea).plusEq(fr);
		idx.plusEq(1008);
		
		df=DiagnosisFactory.parseFile("d:/Data/DiagnosisVortex/"+name+"/"+name+"_CSM.ctl");
		dd=df.getDataDescriptor();
		Variable prs=df.getVariables(new Range("",dd),false,"prs")[0];
		Variable slp=df.getVariables(new Range("",dd),false,"slp")[0];
		
		float[] prsd=prs.getData()[0][0][0];	float[] diffprs=cdiff(prsd);
		float[] slpd=slp.getData()[0][0][0];	float[] diffslp=cdiff(slpd);
		float[] idxd=idx.getData()[0][0][0];	float[] diffidx=cdiff(idxd);
		
		float[] dhd=dh.getData()[0][0][0];
		float[] ead=ea.getData()[0][0][0];
		float[] frd=fr.getData()[0][0][0];
		
		System.out.println(cCorrelationCoefficient(prsd,idxd)+"\t"+cCorrelationCoefficient(diffprs,diffidx));
		System.out.println(cCorrelationCoefficient(slpd,idxd)+"\t"+cCorrelationCoefficient(diffslp,diffidx));
		
		System.out.println();
		for(int l=0;l<dd.getTCount();l++)
		System.out.println(String.format(
			"%9.3f %9.3f %9.3f, %9.3f %9.3f %9.3f, %9.3f %9.3f %9.3f",
			prsd[l],slpd[l],idxd[l],dhd[l],ead[l],frd[l],diffprs[l],diffslp[l],diffidx[l]
		));
	}
	
	public static float[] cdiff(float[] data){
		int N=data.length;
		
		float[] re=new float[N-1];
		
		for(int i=0;i<N-1;i++)
		re[i]=data[i+1]-data[i];
		
		return re;
	}
}
