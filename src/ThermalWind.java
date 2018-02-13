//
import miniufo.application.advanced.CoordinateTransformation;
import miniufo.application.basic.DynamicMethodsInCC;
import miniufo.application.basic.ThermoDynamicMethodsInCC;
import miniufo.application.diagnosticModel.EliassenModelInCC;
import miniufo.descriptor.CsmDescriptor;
import miniufo.descriptor.CtlDescriptor;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.CylindricalSpatialModel;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.SphericalSpatialModel;
import miniufo.diagnosis.Variable;
import miniufo.diagnosis.Variable.Dimension;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;


public class ThermalWind{
	//
	public static void main(String arg[]){
		DiagnosisFactory df=DiagnosisFactory.parseFile("d:/Data/DiagnosisVortex/Haima/Haima.csm");
		DataDescriptor dd=df.getDataDescriptor();
		CtlDescriptor ctl=((CsmDescriptor)dd).getCtlDescriptor();
		CylindricalSpatialModel csm=new CylindricalSpatialModel((CsmDescriptor)dd);
		
		Range r=new Range("t(1,5)",dd);
		
		Variable[] vs=df.getVariables(r,"u","v","w","rh","t","h");
		
		Variable u=vs[0];
		Variable v=vs[1];
		Variable w=vs[2];
		Variable rh=vs[3];
		Variable T=vs[4];
		Variable h=vs[5]; h.multiplyEq(9.8f);
		
		EliassenModelInCC tp=new EliassenModelInCC(csm);
		DynamicMethodsInCC dm=new DynamicMethodsInCC(csm);
		
		CoordinateTransformation ct=new CoordinateTransformation(new SphericalSpatialModel(ctl),csm);
		Variable[] vel=ct.reprojectToCylindrical(u,v);
		
		Variable ut=vel[0];
		Variable vr=vel[1];
		ThermoDynamicMethodsInCC tm=new ThermoDynamicMethodsInCC(csm);
		
		tp.cStormRelativeAziRadVelocity(ut,vr);
		
		Variable q  =tm.cSpecificHumidity(T,rh);
		Variable Th =tm.cPotentialTemperature(T);
		Variable st =tm.cStaticStabilityArgByT(T);
		Variable spt=tm.cStaticStabilityArgByPT(Th);
		Variable Qlh=tm.cLatentHeating(q,u,v,w,T);
		Variable QT =tm.cDiabaticHeatingRateByT(T,u,v,w);
		Variable QPT=tm.cDiabaticHeatingRateByPT(Th,u,v,w);
		Variable gwY=dm.cGradientWindByJohnson(h);
		Variable gwT=dm.cGradientWind(h);
		Variable MaT =dm.cAbsoluteAngularMomentum(ut);
		Variable MaJ =dm.cAbsoluteAngularMomentumByJohnson(ut);
		Variable abW=dm.cAbsoluteAngularVelocity(ut);
		Variable ccf =dm.cCentrifugalCoriolisForce(ut);
		Variable eta =dm.cAbsoluteVorticity(ut,vr);
		Variable a   =tm.cSpecificVolume(T);
		
		ut=ut.anomalizeX(); gwY=gwY.anomalizeX(); Th = Th.anomalizeX();  q= q.anomalizeX(); eta=eta.anomalizeX();
		vr=vr.anomalizeX(); gwT=gwT.anomalizeX(); Qlh=Qlh.anomalizeX(); rh=rh.anomalizeX();
		h = h.anomalizeX(); MaT=MaT.anomalizeX(); QT = QT.anomalizeX();  w= w.anomalizeX();
		T = T.anomalizeX(); MaJ=MaJ.anomalizeX(); QPT=QPT.anomalizeX(); st=st.anomalizeX();
		a = a.anomalizeX(); abW=abW.anomalizeX(); ccf=ccf.anomalizeX();spt=spt.anomalizeX();
		
		Variable gwY2=dm.cMeanGradientWindByJohnson(h);
		Variable gwT2=dm.cMeanGradientWindByCurvedGWB(h);
		Variable abW2=dm.cMeanAbsoluteAngularVelocity(ut);
		Variable MaT2=dm.cMeanAbsoluteAngularMomentum(ut);
		Variable MaJ2=dm.cMeanAbsoluteAngularMomentumByJohnson(ut);
		Variable ccf2=dm.cMeanCentrifugalCoriolisForce(ut);
		Variable ism =dm.cMeanInertialStabilityByUT(ut);
		Variable ism2=dm.cMeanInertialStabilityByAM(MaT2);
		Variable eta2=dm.cMeanAbsoluteVorticity(ut);
		
		Variable MaTY=dm.cDerivative(MaT2,Dimension.Y);
		Variable dCdp=dm.cDerivative(ccf,Dimension.Z);
		Variable dCdp2=dm.cDerivative(ccf2,Dimension.Z);
		Variable dadr=dm.cDerivative(a,Dimension.Y);
		
		gwY.setName("gwY"); gwY2.setName("gwY2");  eta2.setName("eta2");
		gwT.setName("gwT"); gwT2.setName("gwT2");
		abW.setName("abW"); abW2.setName("abW2");
		MaT.setName("MaT"); MaT2.setName("MaT2");
		MaJ.setName("MaJ"); MaJ2.setName("MaJ2");
		ccf.setName("ccf"); ccf2.setName("ccf2");
		ism.setName("ism"); ism2.setName("ism2");
		dCdp.setName("dCdp"); dCdp2.setName("dCdp2");
		dadr.setName("dadr"); MaTY.setName("MaTY");
		
		DataWrite dw=DataIOFactory.getDataWrite(ctl,"D:/ThermalWind.dat");
		dw.writeData(ctl,
			ut,vr,w,h,q,rh,T,Th,a,st,spt,Qlh,QT,eta,eta2,
			QPT,gwY,gwY2,gwT,gwT2,abW,abW2,MaT,MaT2,
			MaJ,MaJ2,ccf,ccf2,dCdp,dCdp2,dadr,ism,ism2,MaTY
		);
		dw.closeFile();
	}
}
