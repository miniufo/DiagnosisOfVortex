//
import miniufo.application.basic.IndexInSC;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.SphericalSpatialModel;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataRead;
import miniufo.io.DataWrite;


public class BlockingIndex{
	//
	public static void main(String[] args){
		DataDescriptor dd=DiagnosisFactory.getDataDescriptor("D:/Data/DiagnosisVortex/catarina/catarina.ctl");
		
		SphericalSpatialModel ssm=new SphericalSpatialModel(dd);
		
		IndexInSC ii=new IndexInSC(ssm);
		
		Range r=new Range("lon(250,350);lat(-80,-5)",dd);
		
		Variable hgt=new Variable("h",r);
		
		DataRead dr=DataIOFactory.getDataRead(dd);
		dr.readData(hgt);	dr.closeFile();
		
		Variable bi=ii.cBlockingIndexByPelly(hgt,6,20);
		
		DataWrite dw=DataIOFactory.getDataWrite(dd,"d:/Data/DiagnosisVortex/catarina/bi.dat");
		dw.writeData(dd,hgt,bi);	dw.closeFile();
	}
}
