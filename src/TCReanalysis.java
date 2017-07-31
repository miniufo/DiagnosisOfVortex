//
import miniufo.application.advanced.CoordinateTransformation;
import miniufo.application.advanced.EllipticEqSORSolver;
import miniufo.application.advanced.EllipticEqSORSolver.DimCombination;
import miniufo.application.basic.DynamicMethodsInCC;
import miniufo.application.basic.ThermoDynamicMethodsInCC;
import miniufo.application.diagnosticModel.EliassenModelInCC;
import miniufo.descriptor.CsmDescriptor;
import miniufo.descriptor.CtlDescriptor;
import miniufo.diagnosis.CylindricalSpatialModel;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.SphericalSpatialModel;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;


//
public class TCReanalysis{
	//
	public static void main(String[] args){
		//DataInterpolation di=
		//new DataInterpolation(DiagnosisFactory.getDataDescriptor("E:/TCReanalysis/Shanshan_original.ctl"));
		//di.verticalInterp("E:/TCReanalysis/Shanshan.dat",37,"tk","rh","u","v","w","geopt");
		//System.exit(0);
		
		DiagnosisFactory df=DiagnosisFactory.parseFile("E:/TCReanalysis/Shanshan.csm");
		CsmDescriptor csmR=(CsmDescriptor)df.getDataDescriptor();
		CsmDescriptor csmC=(CsmDescriptor)DiagnosisFactory.getDataDescriptor("e:/TCReanalysis/Shanshan_JMA.csm");
		CtlDescriptor ctl=csmR.getCtlDescriptor();
		
		Range rlev=new Range("",csmR);
		
		Variable[] vs=df.getVariables(rlev,false,"tk","rh","u","v","w","geopt");
		
		Variable T=vs[0];	Variable rh=vs[1];	Variable h=vs[5];
		Variable u=vs[2];	Variable v =vs[3];	Variable w=vs[4];
		
		/******************************* computation **********************************/
		CylindricalSpatialModel csm=new CylindricalSpatialModel(csmC);
		
		CoordinateTransformation ct=new CoordinateTransformation(
			new SphericalSpatialModel(ctl),csm
		);
		
		EliassenModelInCC ty=new EliassenModelInCC(csm);
		ThermoDynamicMethodsInCC  tm=new ThermoDynamicMethodsInCC(csm);
		DynamicMethodsInCC dm=new DynamicMethodsInCC(csm);
		EllipticEqSORSolver ees=new EllipticEqSORSolver(csm);
		
		Variable s =tm.cStaticStabilityArgByT(T);
		Variable q =tm.cSpecificHumidity(T,rh);
		Variable o =dm.cHydrostaticOmega(w,T);
		Variable th=tm.cPotentialTemperature(T);
		Variable lh=tm.cLatentHeating(q,u,v,o,T);
		
		Variable[] re=ct.reprojectToCylindrical(u,v);
		
		Variable ut=re[0];	Variable vr=re[1];
		
		dm.cStormRelativeAziRadVelocity(ut,vr);
		
		Variable gaz=dm.cAbsoluteAngularMomentumByJohnson(ut);
		
		/************************** tangentially averaging ****************************/
		Variable sm =  s.anomalizeX();
		Variable Tm =  T.anomalizeX();
		Variable rm = rh.anomalizeX();
		Variable hm =  h.anomalizeX();
		Variable um = ut.anomalizeX();
		Variable vm = vr.anomalizeX();
		Variable wm =  w.anomalizeX();
		Variable om =  o.anomalizeX();
		Variable qm =  q.anomalizeX();
		Variable lm = lh.anomalizeX();
		Variable thm= th.anomalizeX();
		Variable gm =gaz.anomalizeX();
		
		Variable tava= th.multiply(vr).anomalizeX(); tava.setName("tava"); tava.setCommentAndUnit("[��'v']");
		Variable tawa= th.multiply(w ).anomalizeX(); tawa.setName("tawa"); tawa.setCommentAndUnit("[��'w']");
		Variable gava=gaz.multiply(vr).anomalizeX(); gava.setName("gava"); gava.setCommentAndUnit("[g'v']");
		Variable gawa=gaz.multiply(w ).anomalizeX(); gawa.setName("gawa"); gawa.setCommentAndUnit("[g'w']");
		
		Variable gw2=dm.cGradientWind(hm);
		Variable gw3=dm.cMeanGradientWindByCurvedGWB(hm);
		
		Variable Am=ty.cA(sm);
		Variable Bm=ty.cB(thm);
		Variable Cm=ty.cC(gm);
		
		Variable f1=ty.cDiabaticHeatingForce(lm);	f1.setCommentAndUnit("forcing of latent heating");
		Variable f4=ty.cEddyHeatHFCForce(tava);
		Variable f5=ty.cEddyHeatVFCForce(tawa);
		Variable f6=ty.cEddyAAMHFCForce(gm,gava);
		Variable f7=ty.cEddyAAMVFCForce(gm,gawa);
		/************** calculate total force ***************/
		Variable ff=f1.copy();	ff.setName("ff"); ff.setCommentAndUnit("total forcing");
		ff=ff.plusEq(f4);	ff=ff.plusEq(f5);
		ff=ff.plusEq(f6);	ff=ff.plusEq(f7);
		
		/************ calculate stream function *************/
		Variable sf=new Variable("sf",vm);
		sf.setCommentAndUnit("streamfunction forced by all internal forcings");
		Variable sf1=sf.copy();	sf1.setName("sf1");
		sf1.setCommentAndUnit("streamfunction forced by all latent heating");
		Variable sf4=sf.copy();	sf4.setName("sf4");
		sf4.setCommentAndUnit("streamfunction forced by eddy heat advection");
		Variable sf5=sf.copy();	sf5.setName("sf5");
		sf5.setCommentAndUnit("streamfunction forced by adiabatic heating");
		Variable sf6=sf.copy();	sf6.setName("sf6");
		sf6.setCommentAndUnit("streamfunction forced by eddy angular momentum horizontal transport");
		Variable sf7=sf.copy();	sf7.setName("sf7");
		sf7.setCommentAndUnit("streamfunction forced by eddy angular momentum vertical transport");
		
		/*** SOR without boundary ***/
		ees.setDimCombination(DimCombination.YZ);
		ees.setABC(ty.cAPrime(sm),ty.cBPrime(thm),ty.cCPrime(gm));
		ees.setMaxLoopCount(2000);
		ees.setTolerance(0.001f);
		
		ees.solve(sf1,f1);
		ees.solve(sf4,f4);
		ees.solve(sf5,f5);
		ees.solve(sf6,f6);
		ees.solve(sf7,f7);
		
		/*** SOR with boundary ***/
		ty.initialSFBoundary(sf,vm,wm);
		ees.solve(sf,ff);
		
		/** calculate radial, vertical velocity components **/
		Variable[] vvf=ty.cVW(sf);
		vvf[0].setName("vs");	vvf[0].setCommentAndUnit("radial wind forced by all forcings");
		vvf[1].setName("ws");	vvf[1].setCommentAndUnit("omega forced by all forcings");
		Variable[] vv1=ty.cVW(sf1);
		vv1[0].setName("vs1");	vv1[0].setCommentAndUnit("radial wind forced by latent heating");
		vv1[1].setName("ws1");	vv1[1].setCommentAndUnit("omega forced by latent heating");
		Variable[] vv4=ty.cVW(sf4);
		vv4[0].setName("vs4");	vv4[0].setCommentAndUnit("radial wind forced by eddy heat advection");
		vv4[1].setName("ws4");	vv4[1].setCommentAndUnit("omega forced by eddy heat advection");
		Variable[] vv5=ty.cVW(sf5);
		vv5[0].setName("vs5");	vv5[0].setCommentAndUnit("radial wind forced by adiabatic heating");
		vv5[1].setName("ws5");	vv5[1].setCommentAndUnit("omega forced by adiabatic heating");
		Variable[] vv6=ty.cVW(sf6);
		vv6[0].setName("vs6");	vv6[0].setCommentAndUnit("radial wind forced by eddy angular momentum horizontal transport");
		vv6[1].setName("ws6");	vv6[1].setCommentAndUnit("omega forced by eddy angular momentum horizontal transport");
		Variable[] vv7=ty.cVW(sf7);
		vv7[0].setName("vs7");	vv7[0].setCommentAndUnit("radial wind forced by eddy angular momentum vertical transport");
		vv7[1].setName("ws7");	vv7[1].setCommentAndUnit("omega forced by eddy angular momentum vertical transport");
		
		DataWrite dw=DataIOFactory.getDataWrite(ctl,"E:/TCReanalysis/model.dat");
		dw.writeData(ctl,
			thm,sm,Tm,rm,hm,um,vm,wm,om,qm,lm,gm,gw2,gw3,Am,Bm,Cm,
			 f1, f4, f5, f6, f7,ff,
			sf1,sf4,sf5,sf6,sf7,sf,
			vvf[0],vv1[0],vv4[0],vv5[0],vv6[0],vv7[0],
			vvf[1],vv1[1],vv4[1],vv5[1],vv6[1],vv7[1]
		);	dw.closeFile();
	}
}
