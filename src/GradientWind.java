//
import miniufo.application.advanced.CoordinateTransformation;
import miniufo.application.basic.DynamicMethodsInCC;
import miniufo.application.diagnosticModel.EliassenModelInCC;
import miniufo.basic.InterpolationModel.Type;
import miniufo.descriptor.CsmDescriptor;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.CylindricalSpatialModel;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.SphericalSpatialModel;
import miniufo.diagnosis.Variable;
import miniufo.io.CsmDataReadStream;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;


public class GradientWind{
	//
	public static void main(String arg[]){
		DiagnosisFactory df=DiagnosisFactory.parseFile("d:/Data/DiagnosisVortex/Haima/Haima.csm");
		CsmDescriptor csd=(CsmDescriptor)df.getDataDescriptor();
		CylindricalSpatialModel csm=new CylindricalSpatialModel(csd);
		SphericalSpatialModel ssm=new SphericalSpatialModel(csd.getCtlDescriptor());
		
		Range range1=new Range("",csd);
		Range range2=new Range("z(1,1)",csd);
		
		Variable u  =new Variable("u"   ,false,range1);
		Variable v  =new Variable("v"   ,false,range1);
		Variable h  =new Variable("h"   ,false,range1);
		Variable sfp=new Variable("psfc",false,range2);
		
		CsmDataReadStream  cdrs=new CsmDataReadStream(csd);
		cdrs.readData(Type.CUBIC_P,u,v,h); h.multiplyEq(9.8f);
		cdrs.readData(Type.CUBIC_P,sfp);
		cdrs.closeFile();
		
		EliassenModelInCC tp=new EliassenModelInCC(csm,sfp);
		DynamicMethodsInCC dm=new DynamicMethodsInCC(csm);
		
		CoordinateTransformation ct=new CoordinateTransformation(ssm,csm);
		Variable[] vel=ct.reprojectToCylindrical(u,v);	u=vel[0];	v=vel[1];
		tp.cStormRelativeAziRadVelocity(u,v);
		
		u=u.anomalizeX();
		v=v.anomalizeX();
		
		Variable hm =h.anomalizeX();
		Variable gdw=dm.cMeanGradientWindByCurvedGWB(hm);
		
		hm.anomalizeY();
		
		DataWrite dw=DataIOFactory.getDataWrite(csd.getCtlDescriptor(),
			"D:/Paper/DiagnosisVortex/Haima/Submission01/Revised(2010.10.25)/Data/Gradient.dat"
		);
		dw.writeData(csd.getCtlDescriptor(),u,v,hm,gdw);	dw.closeFile();
		
		DiagnosisFactory df2=DiagnosisFactory.parseFile(
			"D:/Paper/DiagnosisVortex/Haima/Submission01/Revised(2010.10.25)/Data/Gradient.ctl"
		);
		
		DataDescriptor dd2=df2.getDataDescriptor();
		
		Range r1=new Range("time(2004.9.11.18,2004.9.13.06)",dd2);
		Range r2=new Range("time(2004.9.13.12,2004.9.13.18)",dd2);
		Range r3=new Range("time(2004.9.14.00,2004.9.16.00)",dd2);
		
		Variable gdw1=df2.getVariables(r1,"gw")[0];	gdw1=gdw1.anomalizeT();	gdw1.setName("gw1");
		Variable gdw2=df2.getVariables(r2,"gw")[0];	gdw2=gdw2.anomalizeT();	gdw2.setName("gw2");
		Variable gdw3=df2.getVariables(r3,"gw")[0];	gdw3=gdw3.anomalizeT();	gdw3.setName("gw3");
		
		Variable ut1=df2.getVariables(r1,"ut")[0];	ut1=ut1.anomalizeT();	ut1.setName("ut1");
		Variable ut2=df2.getVariables(r2,"ut")[0];	ut2=ut2.anomalizeT();	ut2.setName("ut2");
		Variable ut3=df2.getVariables(r3,"ut")[0];	ut3=ut3.anomalizeT();	ut3.setName("ut3");
		
		DataWrite dw2=DataIOFactory.getDataWrite(dd2,
			"D:/Paper/DiagnosisVortex/Haima/Submission01/Revised(2010.10.25)/Data/GradientMean.dat"
		);
		dw2.writeData(csd.getCtlDescriptor(),gdw1,gdw2,gdw3,ut1,ut2,ut3);	dw2.closeFile();
	}
}
