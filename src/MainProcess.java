//
import java.io.File;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import miniufo.basic.InterpolationModel.Type;
import miniufo.concurrent.ConcurrentUtil;
import miniufo.descriptor.CsmDescriptor;
import miniufo.descriptor.CtlDescriptor;
import miniufo.descriptor.NetCDFDescriptor;
import miniufo.diagnosis.CylindricalSpatialModel;
import miniufo.diagnosis.SpatialModel;
import miniufo.diagnosis.SphericalSpatialModel;
import miniufo.diagnosis.Variable;
import miniufo.io.CsmDataReadStream;
import miniufo.io.CtlDataReadStream;
import miniufo.io.CtlDataWriteStream;
import miniufo.test.util.FileJoiner;
import miniufo.test.util.OpenGrADS;
import miniufo.util.DataInterpolation;
import miniufo.diagnosis.MDate;
import miniufo.diagnosis.Range;
import miniufo.application.advanced.CoordinateTransformation;
import miniufo.application.advanced.EllipticEqSORSolver;
import miniufo.application.advanced.EllipticEqSORSolver.DimCombination;
import miniufo.application.basic.DynamicMethodsInCC;
import miniufo.application.basic.ThermoDynamicMethodsInCC;
import miniufo.application.diagnosticModel.EliassenModelInCC;


//
public class MainProcess{
	//
	private static final int year=2004;
	private static final boolean sfcFlux  =false;
	private static final boolean  grib2   =false;
	private static final String vortex    ="Haima";
	private static final String csmpath   ="D:/Data/DiagnosisVortex/"+vortex+"/"+vortex+".csm";
	private static final String gribpath  ="//lynn/Data/GRIB/FNL/"+year+"/";
	private static final String gausspath1="//lynn/dataI/NCEP1/Daily4/Surface/shtfl/shtfl.sfc.gauss."+year+".nc";
	private static final String gausspath2="//lynn/dataI/NCEP1/Daily4/Surface/lhtfl/lhtfl.sfc.gauss."+year+".nc";
	private static final String outputpath="D:/Data/DiagnosisVortex/"+vortex+"/";
	
	private static final String[][] sfcVarsGrib={
		{"pres","sfc"},
		{"hgt","sfc"} ,
		{"tmp","2 m above gnd"},
		{"hpbl","sfc"},
		{"ugrd","10 m above gnd"},
		{"vgrd","10 m above gnd"},
	};
	
	private static final String[][] sfcVarsGrib2={
		{"pres","surface"},
		{"hgt","surface"} ,
		{"tmp","2 m above ground"},
		{"hpbl","surface"},
		{"ugrd","10 m above ground"},
		{"vgrd","10 m above ground"},
	};
	
	private static final String[][] prsVarsGrib={
		{"hgt","1000 mb","975 mb","950 mb","925 mb","900 mb","850 mb","800 mb","750 mb","700 mb","650 mb",
		"600 mb","550 mb","500 mb","450 mb","400 mb","350 mb","300 mb","250 mb","200 mb","150 mb","100 mb"},
		{"tmp","1000 mb","975 mb","950 mb","925 mb","900 mb","850 mb","800 mb","750 mb","700 mb","650 mb",
		"600 mb","550 mb","500 mb","450 mb","400 mb","350 mb","300 mb","250 mb","200 mb","150 mb","100 mb"},
		{"vvel","1000 mb","975 mb","950 mb","925 mb","900 mb","850 mb","800 mb","750 mb","700 mb","650 mb",
		"600 mb","550 mb","500 mb","450 mb","400 mb","350 mb","300 mb","250 mb","200 mb","150 mb","100 mb"},
		{"rh","1000 mb","975 mb","950 mb","925 mb","900 mb","850 mb","800 mb","750 mb","700 mb","650 mb",
		"600 mb","550 mb","500 mb","450 mb","400 mb","350 mb","300 mb","250 mb","200 mb","150 mb","100 mb"},
		{"ugrd","1000 mb","975 mb","950 mb","925 mb","900 mb","850 mb","800 mb","750 mb","700 mb","650 mb",
		"600 mb","550 mb","500 mb","450 mb","400 mb","350 mb","300 mb","250 mb","200 mb","150 mb","100 mb"},
		{"vgrd","1000 mb","975 mb","950 mb","925 mb","900 mb","850 mb","800 mb","750 mb","700 mb","650 mb",
		"600 mb","550 mb","500 mb","450 mb","400 mb","350 mb","300 mb","250 mb","200 mb","150 mb","100 mb"},
	};
	
	private static final String[][] prsVarsGrib2=prsVarsGrib;
	
	private static String[][] gribPVars=new String[prsVarsGrib.length][21];
	private static String[]   gribSVars=new String[sfcVarsGrib.length];
	
	private static CsmDescriptor csd=null;
	private static float dis=0;
	private static int interval=50;
	
	private static StringBuffer ctlsb=new StringBuffer();
	
	static{
		try{ csd=new CsmDescriptor(new File(csmpath));}
		catch(Exception e){e.printStackTrace();System.exit(0);}
	}
	
	static{
		boolean test=false;
		if(test)
		try{
			MDate md=csd.getTDef().getSamples()[0];
			
			System.out.println("\nGetting informations of variables from grib data...\n");
			
			Process p=null;
			if(grib2){
				String s=gribpath+getGribFileName(md);
				
				File f=new File(s);
				
				if(f.exists())
					p=Runtime.getRuntime().exec("wgrib2 "+s);
				else
					throw new IllegalArgumentException("file does not exist: "+
						f.getAbsolutePath()
					);
				
			}else{
				String s=gribpath+getGribFileName(md);
				
				File f=new File(s);
				
				if(f.exists())
					p=Runtime.getRuntime().exec("wgrib "+s);
				else
					throw new IllegalArgumentException("file does not exist: "+
						f.getAbsolutePath()
					);
			}
			
			BufferedReader br=new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuffer sb=new StringBuffer();
			String str=null;
			
			while((str=br.readLine())!=null){ sb.append(str); sb.append("\n");}
			
			br.close();
			
			if(grib2){
				for(int v=0;v<sfcVarsGrib2.length;v++){
					Pattern pt=Pattern.compile(
						"(\\d+?(.\\d{1})?)(:\\d+?:d\\=\\d+?:)"+sfcVarsGrib2[v][0]+":"+sfcVarsGrib2[v][1]+":anl:",
						Pattern.CASE_INSENSITIVE
					);
					Matcher m=pt.matcher(sb);
					if(m.find()) gribSVars[v]=m.group(1);
					else throw new IllegalArgumentException("cannot find "+sfcVarsGrib2[v][0]+" in grib file");
				}
				
				for(int v=0;v<prsVarsGrib2.length;v++)
				for(int k=1;k<prsVarsGrib2[0].length;k++){
					Pattern pt=Pattern.compile(
						"(\\d+?(.\\d{1})?)(:\\d+?:d\\=\\d+?:)"+prsVarsGrib2[v][0]+":"+prsVarsGrib2[v][k]+":anl:",
						Pattern.CASE_INSENSITIVE
					);
					Matcher m=pt.matcher(sb);
					if(m.find()) gribPVars[v][k-1]=m.group(1);
					else throw new IllegalArgumentException("cannot find "+prsVarsGrib2[v][0]+":"+prsVarsGrib2[v][k]+" in grib file");
				}
				
			}else{
				for(int v=0;v<sfcVarsGrib.length;v++){
					Pattern pt=Pattern.compile(
						"(\\d+?)(:\\d+?:d\\=\\d+?:)"+sfcVarsGrib[v][0]+"(.+?TimeU\\=\\d+?:)"+sfcVarsGrib[v][1]+":anl:",
						Pattern.CASE_INSENSITIVE
					);
					Matcher m=pt.matcher(sb);
					if(m.find()) gribSVars[v]=m.group(1);
					else throw new IllegalArgumentException("cannot find "+sfcVarsGrib[v][0]+" in grib file");
				}
				
				for(int v=0;v<prsVarsGrib.length;v++)
				for(int k=1;k<prsVarsGrib[0].length;k++){
					Pattern pt=Pattern.compile(
						"(\\d+?)(:\\d+?:d\\=\\d+?:)"+prsVarsGrib[v][0]+"(.+?TimeU\\=\\d+?:)"+prsVarsGrib[v][k]+":anl:",
						Pattern.CASE_INSENSITIVE
					);
					Matcher m=pt.matcher(sb);
					if(m.find()) gribPVars[v][k-1]=m.group(1);
					else throw new IllegalArgumentException("cannot find "+prsVarsGrib[v][0]+":"+prsVarsGrib[v][k]+" in grib file");
				}
			}
			
		}catch(Exception e){e.printStackTrace();System.exit(0);}
	}
	
	//
	private static String getGribFileName(MDate md){
		String year  =String.valueOf(md.getYear());
		String month =String.format("%1$02d",md.getMonth());
		String date  =String.format("%1$02d",md.getDate());
		String hour  =String.format("%1$02d",md.getHour());
		//String minute=String.format("%1$02d",md.getMinute());
		
		return "fnl_"+year+month+date+"_"+hour+"_00";
	}
	
	// data pre-process
	static void dataPreProcess(){
		try{
			System.out.println("Start data Pre-Processing...");
			
			grib2Bin();
			
			if(sfcFlux) fluxDataInterpolation();
			
			joinFiles();
			// delete temporal dat and ctl files
			File fi=null;
			fi=new File(outputpath+vortex+"_grib.dat");	if(fi.exists()) fi.delete();
			fi=new File(outputpath+vortex+"_grib.ctl");	if(fi.exists()) fi.delete();
			fi=new File(outputpath+vortex+"_flux.dat");	if(fi.exists()) fi.delete();
			fi=new File(outputpath+vortex+"_flux.ctl");	if(fi.exists()) fi.delete();
			
			velticalInterpolation();
			// delete temporal dat and ctl files
			fi=new File(outputpath+vortex+"_join.dat");	if(fi.exists()) fi.delete();
			fi=new File(outputpath+vortex+"_join.ctl");	if(fi.exists()) fi.delete();

			System.out.println("Finish data Pre-Process.");
			
		}catch(Exception e){ e.printStackTrace(); System.exit(0);}
	}
	
	// model
	static void modeling(){
		System.out.println("Start simulating...");
		
		
		/*********** delete old dat and ctl files ***********/
		File fi=null;
		fi=new File(outputpath+vortex+"_model.dat"); if(fi.exists()) fi.delete();
		fi=new File(outputpath+vortex+"_model.ctl"); if(fi.exists()) fi.delete();
		fi=new File(outputpath+vortex+"_stn.dat"  ); if(fi.exists()) fi.delete();
		fi=new File(outputpath+vortex+"_stn.ctl"  ); if(fi.exists()) fi.delete();
		fi=new File(outputpath+vortex+"_stn.map"  ); if(fi.exists()) fi.delete();
		
		
		/***************** building models ******************/
		SphericalSpatialModel   ssm=new SphericalSpatialModel(csd.getCtlDescriptor());
		CylindricalSpatialModel csm=new CylindricalSpatialModel(csd);
		
		
		/****************** setting range *******************/
		Range range1=new Range(""      ,csd);
		Range range2=new Range("z(1,1)",csd);
		
		
		/************** initializing variables **************/
		Variable u =new Variable("u" ,true,range1);
		Variable v =new Variable("v" ,true,range1);
		Variable w =new Variable("w" ,true,range1);
		Variable h =new Variable("h" ,true,range1);
		Variable T =new Variable("t" ,true,range1);
		Variable rh=new Variable("rh",true,range1);
		
		Variable u10 =new Variable("u10m",true,range2);
		Variable v10 =new Variable("v10m",true,range2);
		Variable T2m =new Variable("t2m" ,true,range2);
		Variable sfp =new Variable("psfc",true,range2);
		Variable sfh =new Variable("hsfc",true,range2);
		Variable pblh=new Variable("pblh",true,range2);
		Variable shfl=null;
		Variable lhfl=null;
		if(sfcFlux){
			shfl=new Variable("shfl",true,range2);
			lhfl=new Variable("lhfl",true,range2);
		}
		
		
		/****************** reading data ********************/
		CsmDataReadStream  cdrs=new CsmDataReadStream(csd);
		cdrs.readData(Type.CUBIC_P,u,v,w,h,T);
		cdrs.readData(Type.LINEAR,rh);
		if(sfcFlux) cdrs.readData(Type.CUBIC_P,u10,v10,T2m,sfp,sfh,shfl,lhfl);
		else cdrs.readData(Type.CUBIC_P,u10,v10,T2m,sfp,sfh);
		cdrs.readData(Type.LINEAR,pblh);	cdrs.closeFile();
		
		/************** building application ****************/
		EliassenModelInCC  emdl=new EliassenModelInCC(csm,sfp);
		ThermoDynamicMethodsInCC  tdmdl=new ThermoDynamicMethodsInCC(csm);
		DynamicMethodsInCC dmdl=new DynamicMethodsInCC(csm);
		EllipticEqSORSolver ees=new EllipticEqSORSolver(csm);
		
		
		/************** vectors reprojection ****************/
		CoordinateTransformation ct=new CoordinateTransformation(ssm,csm);
		
		Variable uo=u.copy(); // not storm-relative azimuthal-radial wind, for tilting term
		Variable vo=v.copy(); // not storm-relative azimuthal-radial wind, for tilting term
		Variable[] vel=null;
		vel=ct.reprojectToCylindrical(uo ,vo );	uo =vel[0];	vo =vel[1];
		vel=ct.reprojectToCylindrical(u  ,v  );	u  =vel[0];	v  =vel[1];
		vel=ct.reprojectToCylindrical(u10,v10);	u10=vel[0];	v10=vel[1];
		
		emdl.cStormRelativeAziRadVelocity(u  ,v  );
		emdl.cStormRelativeAziRadVelocity(u10,v10);
		
		
		/************** variable calculation ****************/
		Variable q     =tdmdl.cSpecificHumidity(T,rh);
		Variable Tv    =tdmdl.cVirtualTemperature(T,q);
		Variable th    =tdmdl.cPotentialTemperature(T);
		Variable the   =tdmdl.cEquivalentPotentialTemperature(T,q,tdmdl.cLCLTemperature(T,rh));
		Variable lh    =tdmdl.cLatentHeating(q,u,v,w,T);
		Variable Qth   =tdmdl.cDiabaticHeatingRateByPT(th,u,v,w);
		Variable s     =tdmdl.cStaticStabilityArgByPT(th);
		Variable g     = dmdl.cAbsoluteAngularMomentumByJohnson(u);
		Variable[] tau = dmdl.cSurfaceFrictionalStress((mag)->EliassenModelInCC.Cd,u10,v10);
		Variable[] fsfc= dmdl.cSurfaceFriction(tau[0],tau[1],pblh);
		Variable fsfcXL= emdl.assignSurfaceToLevels(fsfc[0],sfh,pblh,h); fsfcXL.setCommentAndUnit("tangential component of surface friction at pressure levels (m s^-2)");
		Variable fsfcYL= emdl.assignSurfaceToLevels(fsfc[1],sfh,pblh,h); fsfcYL.setCommentAndUnit("radial component of surface friction at pressure levels (m s^-2)");
		Variable shrL=null;
		Variable lhrL=null;
		
		if(sfcFlux){
			Variable Tsfc=null;
			Variable shflx=null;
			Variable lhflx=null;
			shrL=emdl.assignSurfaceToLevels(shflx,sfh,pblh,h);
			lhrL=emdl.assignSurfaceToLevels(lhflx,sfh,pblh,h);
			
			shrL.setName("shrL");	shrL.setCommentAndUnit("surface sensible heating rate at pressure level (K s^-1)");
			lhrL.setName("lhrL");	lhrL.setCommentAndUnit("surface  latent  heating rate at pressure level (K s^-1)");
		}
		
		Variable sm   =   s.anomalizeX();
		Variable gm   =   g.anomalizeX();
		Variable qm   =   q.anomalizeX();
		Variable lhm  =  lh.anomalizeX();
		Variable Qthm = Qth.anomalizeX();
		Variable um   =   u.anomalizeX();
		Variable vm   =   v.anomalizeX();
		Variable wm   =   w.anomalizeX();
		Variable Tm   =   T.anomalizeX();
		Variable Tvm  =  Tv.anomalizeX();
		Variable hm   =   h.anomalizeX();
		Variable thm  =  th.anomalizeX();
		Variable them = the.anomalizeX();
		Variable fsXLm=fsfcXL.anomalizeX();
		Variable fsYLm=fsfcYL.anomalizeX();
		Variable fvhm =emdl.cHorizontalViscousFriction(um);
		Variable fvvm =emdl.cVerticalViscousFriction(um);
		Variable gdm  =dmdl.cMeanGradientWindByCurvedGWB(hm.multiplyEq(9.8f));	hm.setCommentAndUnit("geopotential (m^2 s^-2)");
		Variable shrLm=null;
		Variable lhrLm=null;
		if(sfcFlux){
			shrLm=shrL.anomalizeX();
			lhrLm=lhrL.anomalizeX();
		}
		
		Variable Am=emdl.cA(sm );
		Variable Bm=emdl.cB(thm);
		Variable Cm=emdl.cC(gm );
		Variable BsinB=emdl.weightBSin(Bm);
		Variable CsinB=emdl.weightBSin(Cm);
		BsinB.setName("BsinB"); BsinB.setComment("B * sin(beta) (m^2 kg^-1)");
		CsinB.setName("CsinB"); CsinB.setComment("C * sin(beta) (s^-2)");
		
		
		/************** eddy calculation ****************/
		Variable tava=th.multiply(v); tava.setName("tava"); tava.setCommentAndUnit("eddy heat ¦È'v' (K m s^-1)");
		Variable tawa=th.multiply(w); tawa.setName("tawa"); tawa.setCommentAndUnit("eddy heat ¦È'w' (K Pa s^-1)");
		Variable gava= g.multiply(v); gava.setName("gava"); gava.setCommentAndUnit("eddy AAM  g'v' (m^3 s^-2)");
		Variable gawa= g.multiply(w); gawa.setName("gawa"); gawa.setCommentAndUnit("eddy AAM  g'w' (m^2 Pa s^-2)");
		
		//CsmDataWriteStream cs=new CsmDataWriteStream(outputpath+vortex+"_stn.dat");
		//cs.writeData(csd,sw,gv,gw,g,u,v,T,a);	cs.closeFile();
		
		
		/**************** eddy flux calculation *****************/
		Variable tavam=tava.anomalizeX(); tavam.setName("tavam"); tavam.setCommentAndUnit("eddy heat horizontal flux [¦È'v'] (K m s^-1)");
		Variable tawam=tawa.anomalizeX(); tawam.setName("tawam"); tawam.setCommentAndUnit("eddy heat  vertical  flux [¦È'w'] (K Pa s^-1)");
		Variable gavam=gava.anomalizeX(); gavam.setName("gavam"); gavam.setCommentAndUnit("eddy AAM  horizontal flux [g'v'] (m^3 s^-2)");
		Variable gawam=gawa.anomalizeX(); gawam.setName("gawam"); gawam.setCommentAndUnit("eddy AAM   vertical  flux [g'w'] (m^2 Pa s^-2)");
		Variable gavaR=dmdl.deWeightBSin(gavam).divideEq(SpatialModel.EARTH_RADIUS); gavaR.setName("gavaR"); gavaR.setCommentAndUnit("eddy AAM horizontal flux divided by R [g'v']/r (m^2 s^-2)");
		Variable gawaR=dmdl.deWeightBSin(gawam).divideEq(SpatialModel.EARTH_RADIUS); gawaR.setName("gawaR"); gawaR.setCommentAndUnit("eddy AAM  vertical  flux divided by R [g'w']/r (m Pa s^-2)");
		
		Variable sfe   =emdl.cEddyInducedStreamfunction(tavam,thm);
		Variable sfe2   =emdl.cCoordinateIndependentEddyInducedStreamfunction(tavam,tawam,thm); sfe2.setName("sfe2");
		Variable[] vedy=emdl.cEddyInducedVelocity(sfe);
		Variable[] vedy2=emdl.cEddyInducedVelocity(sfe2); vedy2[0].setName("vedy2"); vedy2[1].setName("wedy2");
		Variable[] EPVector=emdl.cEPVector(tavam,gavam,gawam,gm,thm,0.8f);
		Variable EPDiv =dmdl.cYZDivergence(EPVector[0],EPVector[1]); EPDiv.setName("EPDiv"); EPDiv.setCommentAndUnit("EP flux divergence (m^2 s^-2)");
		Variable EPDivR=dmdl.deWeightBSin(EPDiv).divideEq(SpatialModel.EARTH_RADIUS); EPDivR.setName("EPDivR"); EPDivR.setCommentAndUnit("EP flux divergence divided by R (m s^-2)");
		
		
		/**************** force calculation *****************/
		Variable f01=emdl.cDiabaticHeatingForce(Qthm);	f01.setCommentAndUnit("force due to latent heating (m^2 kg^-1 s^-1)");
		Variable f02=null;
		Variable f03=null;
		if(sfcFlux){
			f02=emdl.cDiabaticHeatingForce(shrLm); f02.setName("shrFor"); f02.setCommentAndUnit("force due to sensible heat flux (m^2 kg^-1 s^-1)");
			f03=emdl.cDiabaticHeatingForce(lhrLm); f03.setName("lhrFor"); f03.setCommentAndUnit("force due to  latent  heat flux (m^2 kg^-1 s^-1)");
		}
		Variable f04=emdl.cEddyHeatHFCForce(tavam);
		Variable f05=emdl.cEddyHeatVFCForce(tawam);
		Variable f06=emdl.cEddyAAMHFCForce(gm,gavam);
		Variable f07=emdl.cEddyAAMVFCForce(gm,gawam);
		Variable f08=emdl.cFrictionalTorqueForce(gm,fsXLm); f08.setName("fsfFor"); f08.setCommentAndUnit("force due to surface frictional torque (m^2 kg^-1 s^-1)");
		Variable f09=emdl.cFrictionalTorqueForce(gm,fvhm ); f09.setName("fvhFor"); f09.setCommentAndUnit("force due to horizontal viscous torque (m^2 kg^-1 s^-1)");
		Variable f10=emdl.cFrictionalTorqueForce(gm,fvvm ); f10.setName("fvvFor"); f10.setCommentAndUnit("force due to  vertical  viscous torque (m^2 kg^-1 s^-1)");
		Variable f11=emdl.cTiltingForce(gm,uo,vo);
		
		Variable dhr   =emdl.cDiabaticHeatingRate(Qthm);
		Variable htHFC =emdl.cEddyHeatHFC(tavam);
		Variable htVFC =emdl.cEddyHeatVFC(tawam);
		Variable aamHFC=emdl.cEddyAAMHFC(gavam);
		Variable aamVFC=emdl.cEddyAAMVFC(gawam);
		Variable aamHFCR=dmdl.deWeightBSin(aamHFC).divideEq(SpatialModel.EARTH_RADIUS); aamHFCR.setName("aamHFCR"); aamHFCR.setCommentAndUnit("AAM HFC dividing R (m s^-2)");
		Variable aamVFCR=dmdl.deWeightBSin(aamVFC).divideEq(SpatialModel.EARTH_RADIUS); aamVFCR.setName("aamVFCR"); aamVFCR.setCommentAndUnit("AAM VFC dividing R (m s^-2)");
		Variable frictq=emdl.cFrictionalTorque(fsXLm); frictq.setName("frictq"); frictq.setCommentAndUnit("surface frictional torque (m^2 s^-2)");
		Variable fvhmtq=emdl.cFrictionalTorque(fsXLm); fvhmtq.setName("fvhmtq"); fvhmtq.setCommentAndUnit("horizontal viscous frictional torque (m^2 s^-2)");
		Variable fvvmtq=emdl.cFrictionalTorque(fsXLm); fvvmtq.setName("fvvmtq"); fvvmtq.setCommentAndUnit("vertical   viscous frictional torque (m^2 s^-2)");
		Variable tilt  =emdl.cTilting(uo,vo);
		
		Variable f01f=emdl.cHeatFF(dhr);
		Variable f04f=emdl.cHeatFF(htHFC);
		Variable f05f=emdl.cHeatFF(htVFC);
		Variable f06f=emdl.cMomentumFF(gm,aamHFC);
		Variable f07f=emdl.cMomentumFF(gm,aamVFC);
		Variable f08f=emdl.cMomentumFF(gm,frictq); f08f.setName("fsfFF"); f08f.setCommentAndUnit("forcing factor due to surface frictional torque (m s^-3)");
		Variable f09f=emdl.cMomentumFF(gm,fvhmtq); f09f.setName("fhvFF"); f09f.setCommentAndUnit("forcing factor due to horizontal viscous torque (m s^-3)");
		Variable f10f=emdl.cMomentumFF(gm,fvvmtq); f10f.setName("fvvFF"); f10f.setCommentAndUnit("forcing factor due to vertical   viscous torque (m s^-3)");
		Variable f11f=emdl.cMomentumFF(gm,tilt);
		
		
		/********** calculate thermodynamic force ***********/
		Variable fat=f01.copy();	fat.setName("fat"); fat.setCommentAndUnit("all thermal dynamic force (m^2 kg^-1 s^-1)");
		if(sfcFlux){ fat.plusEq(f02); fat.plusEq(f03);}
		fat.plusEq(f04); fat.plusEq(f05);
		
		
		/************* calculate dynamic force **************/
		Variable fad=f06.copy();	fad.setName("fad"); fad.setCommentAndUnit("all dynamic force (m^2 kg^-1 s^-1)");
		fad.plusEq(f07); fad.plusEq(f08); fad.plusEq(f09); fad.plusEq(f10); fad.plusEq(f11);
		
		
		/************** calculate total force ***************/
		Variable faf=f01.copy();	faf.setName("faf"); faf.setCommentAndUnit("total force (m^2 kg^-1 s^-1)");
		if(sfcFlux){ faf.plusEq(f02); faf.plusEq(f03);}
		faf.plusEq(f04); faf.plusEq(f05);
		faf.plusEq(f06); faf.plusEq(f07); faf.plusEq(f08); faf.plusEq(f09); faf.plusEq(f10); faf.plusEq(f11);
		
		
		/************ calculate stream function *************/
		Variable sffr=new Variable("sffr",vm); sffr.setCommentAndUnit("streamfunction forced by all internal forcings (Pa m s^-1)");
		Variable sf01=new Variable("sf01",vm); sf01.setCommentAndUnit("streamfunction forced by latent heating (Pa m s^-1)");
		Variable sf02=new Variable("sf02",vm); sf02.setCommentAndUnit("streamfunction forced by sensible heat flux (Pa m s^-1)");
		Variable sf03=new Variable("sf03",vm); sf03.setCommentAndUnit("streamfunction forced by latent   heat flux (Pa m s^-1)");
		Variable sf04=new Variable("sf04",vm); sf04.setCommentAndUnit("streamfunction forced by eddy heat HFC (Pa m s^-1)");
		Variable sf05=new Variable("sf05",vm); sf05.setCommentAndUnit("streamfunction forced by eddy heat VFC (Pa m s^-1)");
		Variable sf06=new Variable("sf06",vm); sf06.setCommentAndUnit("streamfunction forced by eddy AAM  HFC (Pa m s^-1)");
		Variable sf07=new Variable("sf07",vm); sf07.setCommentAndUnit("streamfunction forced by eddy AAM  VFC (Pa m s^-1)");
		Variable sf08=new Variable("sf08",vm); sf08.setCommentAndUnit("streamfunction forced by surface frictional torque (Pa m s^-1)");
		Variable sf09=new Variable("sf09",vm); sf09.setCommentAndUnit("streamfunction forced by horizontal viscous torque (Pa m s^-1)");
		Variable sf10=new Variable("sf10",vm); sf10.setCommentAndUnit("streamfunction forced by vertical   viscous torque (Pa m s^-1)");
		Variable sf11=new Variable("sf11",vm); sf11.setCommentAndUnit("streamfunction forced by tilting effect (Pa m s^-1)");
		Variable sftf=new Variable("sftf",vm); sftf.setCommentAndUnit("streamfunction forced by all thermal forces (Pa m s^-1)");
		Variable sfdf=new Variable("sfdf",vm); sfdf.setCommentAndUnit("streamfunction forced by all dynamic forces (Pa m s^-1)");
		Variable sfbd=new Variable("sfbd",vm); sfbd.setCommentAndUnit("streamfunction forced by boundary effect (Pa m s^-1)");
		Variable sfsm=new Variable("sfsm",vm); sfsm.setCommentAndUnit("streamfunction forced by all internal forcings and boundary effect (Pa m s^-1)");
		
		/*** SOR without boundary ***/
		ees.setDimCombination(DimCombination.YZ);
		ees.setABC(emdl.cAPrime(sm),emdl.cBPrime(thm),emdl.cCPrime(gm));
		ees.setTolerance(1e-10);
		
		ees.solve(sf01,f01);
		if(sfcFlux){
			ees.solve(sf02,f02);
			ees.solve(sf03,f03);
		}
		ees.solve(sf04,f04);
		ees.solve(sf05,f05);
		ees.solve(sf06,f06);
		ees.solve(sf07,f07);
		ees.solve(sf08,f08);
		ees.solve(sf09,f09);
		ees.solve(sf10,f10);
		ees.solve(sf11,f11);
		ees.solve(sftf,fat );
		ees.solve(sfdf,fad );
		ees.solve(sffr,faf );
		
		/*** SOR with boundary ***/
		emdl.initialSFBoundary(sfbd,vm,wm);
		emdl.initialSFBoundary(sfsm,vm,wm);
		ees.solve(sfbd,null);
		ees.solve(sfsm,faf);
		
		
		/** calculate radial, vertical velocity components **/
		Variable[] vvfr=emdl.cVW(sffr);
		vvfr[0].setName("vsfr");	vvfr[0].setCommentAndUnit("radial wind forced by all internal forcings (m s^-1)");
		vvfr[1].setName("wsfr");	vvfr[1].setCommentAndUnit("omega forced by all internal forcings (Pa s^-1)");
		Variable[] vv01=emdl.cVW(sf01);
		vv01[0].setName("vs01");	vv01[0].setCommentAndUnit("radial wind forced by latent heating (m s^-1)");
		vv01[1].setName("ws01");	vv01[1].setCommentAndUnit("omega forced by latent heating (Pa s^-1)");
		Variable[] vv02=emdl.cVW(sf02);
		vv02[0].setName("vs02");	vv02[0].setCommentAndUnit("radial wind forced by sensible heat flux (m s^-1)");
		vv02[1].setName("ws02");	vv02[1].setCommentAndUnit("omega forced by sensible heat flux (Pa s^-1)");
		Variable[] vv03=emdl.cVW(sf03);
		vv03[0].setName("vs03");	vv03[0].setCommentAndUnit("radial wind forced by latent heat flux (m s^-1)");
		vv03[1].setName("ws03");	vv03[1].setCommentAndUnit("omega forced by latent heat flux (Pa s^-1)");
		Variable[] vv04=emdl.cVW(sf04);
		vv04[0].setName("vs04");	vv04[0].setCommentAndUnit("radial wind forced by eddy heat HFC (m s^-1)");
		vv04[1].setName("ws04");	vv04[1].setCommentAndUnit("omega forced by eddy heat HFC (Pa s^-1)");
		Variable[] vv05=emdl.cVW(sf05);
		vv05[0].setName("vs05");	vv05[0].setCommentAndUnit("radial wind forced by eddy heat VFC (m s^-1)");
		vv05[1].setName("ws05");	vv05[1].setCommentAndUnit("omega forced by eddy heat VFC (Pa s^-1)");
		Variable[] vv06=emdl.cVW(sf06);
		vv06[0].setName("vs06");	vv06[0].setCommentAndUnit("radial wind forced by eddy AAM HFC (m s^-1)");
		vv06[1].setName("ws06");	vv06[1].setCommentAndUnit("omega forced by eddy AAM HFC (Pa s^-1)");
		Variable[] vv07=emdl.cVW(sf07);
		vv07[0].setName("vs07");	vv07[0].setCommentAndUnit("radial wind forced by eddy AAM VFC (m s^-1)");
		vv07[1].setName("ws07");	vv07[1].setCommentAndUnit("omega forced by eddy AAM VFC (Pa s^-1)");
		Variable[] vv08=emdl.cVW(sf08);
		vv08[0].setName("vs08");	vv08[0].setCommentAndUnit("radial wind forced by surface frictional torque (m s^-1)");
		vv08[1].setName("ws08");	vv08[1].setCommentAndUnit("omega forced by surface frictional torque (Pa s^-1)");
		Variable[] vv09=emdl.cVW(sf09);
		vv09[0].setName("vs09");	vv09[0].setCommentAndUnit("radial wind forced by horizontal viscous torque (m s^-1)");
		vv09[1].setName("ws09");	vv09[1].setCommentAndUnit("omega forced by horizontal viscous torque (Pa s^-1)");
		Variable[] vv10=emdl.cVW(sf10);
		vv10[0].setName("vs10");	vv10[0].setCommentAndUnit("radial wind forced by vertical viscous torque (m s^-1)");
		vv10[1].setName("ws10");	vv10[1].setCommentAndUnit("omega forced by vertical viscous torque (Pa s^-1)");
		Variable[] vv11=emdl.cVW(sf11);
		vv11[0].setName("vs11");	vv11[0].setCommentAndUnit("radial wind forced by tilting effect (m s^-1)");
		vv11[1].setName("ws11");	vv11[1].setCommentAndUnit("omega forced by tilting effect (Pa s^-1)");
		Variable[] vvtf=emdl.cVW(sftf);
		vvtf[0].setName("vstf");	vvtf[0].setCommentAndUnit("radial wind forced by all thermal forces (m s^-1)");
		vvtf[1].setName("wstf");	vvtf[1].setCommentAndUnit("omega forced by all thermal forces (Pa s^-1)");
		Variable[] vvdf=emdl.cVW(sfdf);
		vvdf[0].setName("vsdf");	vvdf[0].setCommentAndUnit("radial wind forced by all dynamic forces (m s^-1)");
		vvdf[1].setName("wsdf");	vvdf[1].setCommentAndUnit("omega forced by all dynamic forces (Pa s^-1)");
		Variable[] vvbd=emdl.cVW(sfbd);
		vvbd[0].setName("vsbd");	vvbd[0].setCommentAndUnit("radial wind forced by boundary effect (m s^-1)");
		vvbd[1].setName("wsbd");	vvbd[1].setCommentAndUnit("omega forced by boundary effect (Pa s^-1)");
		Variable[] vvsm=emdl.cVW(sfsm );
		vvsm[0].setName("vsm" );	vvsm[0].setCommentAndUnit("radial wind forced by all (m s^-1)");
		vvsm[1].setName("wsm" );	vvsm[1].setCommentAndUnit("omega forced by all (Pa s^-1)");
		
		
		/******************** data output *******************/
		CtlDataWriteStream cdws=new CtlDataWriteStream(outputpath+vortex+"_model.dat");
		if(sfcFlux)
		cdws.writeData(csd.getCtlDescriptor(),
			  sm,  gm, gdm,  qm, lhm,Qthm,  um,  vm,  wm,  Am,  Bm,  Cm,  Tm,Tvm,thm,them,hm,fsXLm,fsYLm,
			fvhm,fvvm,shrLm,lhrLm,tavam,tawam,gavam,gawam,dhr,htHFC,htVFC,aamHFC,aamVFC,aamHFCR,aamVFCR,frictq,fvhmtq,fvvmtq,tilt,
			f01f,f04f,f05f,f06f,f07f,f08f,f09f,f10f,f11f,sfe ,vedy[0],vedy[1],EPVector[0],EPVector[1],EPVector[2],EPVector[3],EPDiv,EPDivR,
			 faf, fat, fad, f01, f02, f03, f04, f05, f06, f07, f08, f09, f10, f11,
			sffr,sftf,sfdf,sf01,sf02,sf03,sf04,sf05,sf06,sf07,sf08,sf09,sf10,sf11,sfbd,sfsm,
			vvfr[0],vv01[0],vv02[0],vv03[0],vv04[0],vv05[0],vv06[0],vv07[0],vv08[0],vv09[0],vv10[0],vv11[0],vvtf[0],vvdf[0],vvbd[0],vvsm[0],
			vvfr[1],vv01[1],vv02[1],vv03[1],vv04[1],vv05[1],vv06[1],vv07[1],vv08[1],vv09[1],vv10[1],vv11[1],vvtf[1],vvdf[1],vvbd[1],vvsm[1]
		);
		else
		cdws.writeData(csd.getCtlDescriptor(),
			  sm,  gm, gdm,  qm, lhm,Qthm,  um,  vm, wm , Am , Bm , Cm ,BsinB,CsinB, Tm ,Tvm,thm,them, hm,fsXLm,fsYLm,
			fvhm,fvvm,tavam,tawam,gavam,gawam,gavaR,gawaR,dhr,htHFC,htVFC,aamHFC,aamVFC,aamHFCR,aamVFCR,frictq,fvhmtq,fvvmtq,tilt,
			f01f,f04f,f05f,f06f,f07f,f08f,f09f,f10f,f11f,sfe ,sfe2,vedy[0],vedy[1],vedy2[0],vedy2[1],EPVector[0],EPVector[1],EPVector[2],EPVector[3],EPDiv,EPDivR,
			 faf, fat, fad, f01, f04, f05, f06, f07, f08, f09, f10, f11,
			sffr,sftf,sfdf,sf01,sf02,sf03,sf04,sf05,sf06,sf07,sf08,sf09,sf10,sf11,sfbd,sfsm,
			vvfr[0],vv01[0],vv02[0],vv03[0],vv04[0],vv05[0],vv06[0],vv07[0],vv08[0],vv09[0],vv10[0],vv11[0],vvtf[0],vvdf[0],vvbd[0],vvsm[0],
			vvfr[1],vv01[1],vv02[1],vv03[1],vv04[1],vv05[1],vv06[1],vv07[1],vv08[1],vv09[1],vv10[1],vv11[1],vvtf[1],vvdf[1],vvbd[1],vvsm[1]
		);
		cdws.closeFile();
		
		System.out.println("Finish simulating.");
	}
	
	// print
	static void printing(){ OpenGrADS.runGS(outputpath+vortex+".gs");}
	
	
	/**************************** data pre-process **********************************/
	private static void fluxDataInterpolation() throws IOException{
		System.out.println("Start interpolating flux data from gauss grid to lon/lat grid...");
		
		// delete old files
		File fi=null;
		fi=new File(outputpath+vortex+"_flux.dat" );	if(fi.exists()) fi.delete();
		fi=new File(outputpath+vortex+"_flux.ctl" );	if(fi.exists()) fi.delete();
		fi=new File(outputpath+vortex+"_flux1.dat");	if(fi.exists()) fi.delete();
		fi=new File(outputpath+vortex+"_flux1.ctl");	if(fi.exists()) fi.delete();
		fi=new File(outputpath+vortex+"_flux2.dat");	if(fi.exists()) fi.delete();
		fi=new File(outputpath+vortex+"_flux2.ctl");	if(fi.exists()) fi.delete();
		
		DataInterpolation di=null;
		di=new DataInterpolation(new NetCDFDescriptor(gausspath1));
		di.GaussianToEvenGridInterp(outputpath+vortex+"_flux1.dat",Type.PERIODIC_CUBIC_P,181,360);
		di=new DataInterpolation(new NetCDFDescriptor(gausspath2));
		di.GaussianToEvenGridInterp(outputpath+vortex+"_flux2.dat",Type.PERIODIC_CUBIC_P,181,360);
		
		//extract data corresponding to the time range
		CtlDescriptor ctl1=new CtlDescriptor(new File(outputpath+vortex+"_flux1.ctl"));
		CtlDescriptor ctl2=new CtlDescriptor(new File(outputpath+vortex+"_flux2.ctl"));
		
		int st=ctl1.getTNum(csd.getTDef().getSamples()[0])+1;
		
		Range r=new Range("t("+st+","+(st+csd.getTCount()-1)+")",ctl1);
		
		System.out.println(
			"Start extracting flux data ("+csd.getTDef().getSamples()[0]+
			" ~~~ "+ctl1.getTDef().getSamples()[st+csd.getTCount()-2]+")..."
		);
		
		Variable var1=new Variable("shtfl",r);
		Variable var2=new Variable("lhtfl",r);
		
		CtlDataReadStream cdrs=new CtlDataReadStream(ctl1);
		cdrs.readData(var1);	cdrs.closeFile();
		
		cdrs=new CtlDataReadStream(ctl2);
		cdrs.readData(var2);	cdrs.closeFile();
		
		CtlDataWriteStream cdws=new CtlDataWriteStream(outputpath+vortex+"_flux.dat");
		cdws.writeData(var1);	cdws.writeData(var2);
		cdws.writeCtl(ctl1);	cdws.closeFile();
		
		// delete temporal dat and ctl files
		fi=null;
		fi=new File(outputpath+vortex+"_flux1.dat");	if(fi.exists()) fi.delete();
		fi=new File(outputpath+vortex+"_flux2.dat");	if(fi.exists()) fi.delete();
		fi=new File(outputpath+vortex+"_flux1.ctl");	if(fi.exists()) fi.delete();
		fi=new File(outputpath+vortex+"_flux2.ctl");	if(fi.exists()) fi.delete();
		
		System.out.println("Finish processing flux data.");
	}
	
	private static void grib2Bin()
	throws FileNotFoundException,IOException,InterruptedException{
		System.out.println("Start extracting grib data to binary data...");
		
		// delete old files
		File fi=null;
		fi=new File(outputpath+vortex+"_grib.dat");	if(fi.exists()) fi.delete();
		fi=new File(outputpath+vortex+"_grib.ctl");	if(fi.exists()) fi.delete();
		
		int tcount=csd.getTCount();	String increment=csd.getTIncrement();
		
		// get grib file names
		MDate md=csd.getTDef().getSamples()[0].copy();
		String[] gribnames=new String[tcount];
		if(!grib2){
			gribnames[0]=getGribFileName(md);
			
			for(int i=1;i<tcount;i++){
				md=md.add(increment);
				gribnames[i]=getGribFileName(md);
			}
			
		}else{
			gribnames[0]=getGribFileName(md);
			
			for(int i=1;i<tcount;i++){
				md=md.add(increment);
				gribnames[i]=getGribFileName(md);
			}
		}
		
		/************ construct bat file (surface and pressure data) ************/
		System.out.println("Generating bat file...");
		
		// extract surface data
		StringBuffer sb=new StringBuffer();
		if(!grib2)
			for(int i=0;i<tcount;i++){
				File f=new File(gribpath+gribnames[i]);
				
				if(f.exists()){
					for(int m=0;m<gribSVars.length;m++)
					sb.append(
						"wgrib -v -d "+gribSVars[m]+" "+gribpath+gribnames[i]+
						" -nh -bin -append -o "+outputpath+vortex+"_grib.dat\n"
					);
					
					// extract pressure data (21 levels)
					for(int m=0;m<gribPVars.length;m++)
					for(int k=0;k<21;k++)
					sb.append(
						"wgrib -v -d "+gribPVars[m][k]+" "+gribpath+gribnames[i]+
						" -nh -bin -append -o "+outputpath+vortex+"_grib.dat\n"
					);
					
				}else throw new FileNotFoundException(gribpath+gribnames[i]);
			}
		else
			for(int i=0;i<tcount;i++){
				File f=new File(gribpath+gribnames[i]);
				
				if(f.exists()){
					for(int m=0;m<gribSVars.length;m++)
					sb.append(
						"wgrib2 -v -d "+gribSVars[m]+" "+gribpath+gribnames[i]+
						" -no_header -append -bin "+outputpath+vortex+"_grib.dat\n"
					);
					
					// extract pressure data (21 levels)
					for(int m=0;m<gribPVars.length;m++)
					for(int k=0;k<21;k++)
					sb.append(
						"wgrib2 -v -d "+gribPVars[m][k]+" "+gribpath+gribnames[i]+
						" -no_header -append -bin "+outputpath+vortex+"_grib.dat\n"
					);
					
				}else throw new FileNotFoundException(gribpath+gribnames[i]);
			}
		
		// generate bat file
		FileWriter fw=new FileWriter(outputpath+"extract.bat");
		fw.write(sb.toString());	fw.close();
		/************ construct bat file (surface and pressure data) ************/
		
		
		/*************************** run bat file ***************************/
		System.out.println("Running bat file, please wait...");
		
		Process p=Runtime.getRuntime().exec(outputpath+"extract.bat");
		
		BufferedReader br=new BufferedReader(new InputStreamReader(p.getInputStream()));
		String str=br.readLine();
		if(!grib2)
		while(str!=null){
			System.out.println(br.readLine().replaceFirst("^.+\\>(?=wgrib )","")+" "+br.readLine());
			br.readLine();	str=br.readLine();
		}
		else
		while(str!=null){
			System.out.println(br.readLine().replaceFirst("^.+\\>(?=wgrib2 )","")+" "+br.readLine());
			str=br.readLine();
		}
		br.close();
		
		// delete bat file
		File fl=new File(outputpath+"extract.bat");	if(fl.exists()) fl.delete();
		/*************************** run bat file ***************************/
		
		
		// write new ctl
		sb.setLength(0);
		sb.append("dset "+outputpath+vortex+"_grib.dat\n");
		if(!grib2) sb.append("options yrev\n");
		sb.append("undef 9.999E+20\n");
		sb.append("title grib data of "+vortex+"\n");
		sb.append("xdef  360 linear    0    1\n");
		sb.append("ydef  181 linear  -90    1\n");
		sb.append("tdef   "+csd.getTCount()+" linear "+csd.getTDef().getSamples()[0].toGradsDate()+" "+csd.getTIncrement()+"\n");
		sb.append("zdef   21 levels 1000 975 950 925 900 850 800 750 700 ");
		sb.append("650 600 550 500 450 400 350 300 250 200 150 100\n");
		sb.append("vars 12\n");
		sb.append("psfc   0   99 pressure of surface (Pa)\n");
		sb.append("hsfc   0   99 hgt of surface (m)\n");
		sb.append("t2m    0   99 temperature of 2 m above surface (K)\n");
		sb.append("pblh   0   99 planetary boundary layer height (m)\n");
		sb.append("u10m   0   99 uwnd of 10 m above surface (m/s)\n");
		sb.append("v10m   0   99 vwnd of 10 m above surface (m/s)\n");
		sb.append("h     21   99 hgt (m)\n");
		sb.append("t     21   99 temperature (K)\n");
		sb.append("w     21   99 vertical velocity (Pa/s)\n");
		sb.append("rh    21   99 relative humidity (%)\n");
		sb.append("u     21   99 uwnd (m/s)\n");
		sb.append("v     21   99 vwnd (m/s)\n");
		sb.append("endvars\n");
		
		fw=new FileWriter(outputpath+vortex+"_grib.ctl");
		fw.write(sb.toString());	fw.close();
		
		System.out.println("Finish extracting.");
	}
	
	private static void joinFiles() throws IOException{
		//delete old dat and ctl files
		File fi=null;
		fi=new File(outputpath+vortex+".dat"     );	if(fi.exists()) fi.delete();
		fi=new File(outputpath+vortex+".ctl"     );	if(fi.exists()) fi.delete();
		fi=new File(outputpath+vortex+"_join.dat");	if(fi.exists()) fi.delete();
		fi=new File(outputpath+vortex+"_join.ctl");	if(fi.exists()) fi.delete();
		
		boolean isYRev=false;
		
		if(!sfcFlux){
			fi=new File(outputpath+vortex+"_grib.dat");
			if(fi.exists()) fi.renameTo(new File(outputpath+vortex+"_join.dat"));
			fi=new File(outputpath+vortex+"_grib.ctl");
			if(fi.exists()) fi.delete();
			
			if(!grib2) isYRev=true;
			
		}else{
			FileJoiner fj=new FileJoiner(outputpath+vortex+"_join.dat");
			fj.joinFile(new CtlDescriptor[]{
				new CtlDescriptor(new File(outputpath+vortex+"_flux.ctl")),
				new CtlDescriptor(new File(outputpath+vortex+"_grib.ctl"))
			});
			
			fj.closeFile();
		}
		
		// write new ctl
		StringBuffer sb=new StringBuffer();
		sb.append("dset "+outputpath+vortex+"_join.dat\n");
		if(isYRev) sb.append("options yrev\n");
		sb.append("undef 9.999E+20\n");
		sb.append("title grib data of "+vortex+"\n");
		sb.append("xdef  360 linear    0    1\n");
		sb.append("ydef  181 linear  -90    1\n");
		sb.append("tdef   "+csd.getTCount()+" linear "+
			csd.getTDef().getSamples()[0].toGradsDate()+" "+csd.getTIncrement()+"\n"
		);
		sb.append("zdef   21 levels 1000 975 950 925 900 850 800 750 700 ");
		sb.append("650 600 550 500 450 400 350 300 250 200 150 100\n");
		if(sfcFlux){
			sb.append("vars 14\n");
			sb.append("shfl   0   99 surface sensible heat flux (w/m^2)\n");
			sb.append("lhfl   0   99 surface latent heat flux (w/m^2)\n");
		}else{
			sb.append("vars 12\n");
		}
		sb.append("psfc   0   99 pressure of surface (Pa)\n");
		sb.append("hsfc   0   99 hgt of surface (m)\n");
		sb.append("t2m    0   99 temperature of 2 m above surface (K)\n");
		sb.append("pblh   0   99 planetary boundary layer height (m)\n");
		sb.append("u10m   0   99 uwnd of 10 m above surface (m/s)\n");
		sb.append("v10m   0   99 vwnd of 10 m above surface (m/s)\n");
		sb.append("h     21   99 hgt (m)\n");
		sb.append("t     21   99 temperature (K)\n");
		sb.append("w     21   99 vertical velocity (Pa/s)\n");
		sb.append("rh    21   99 relative humidity (%)\n");
		sb.append("u     21   99 uwnd (m/s)\n");
		sb.append("v     21   99 vwnd (m/s)\n");
		sb.append("endvars\n");
		
		FileWriter fw=new FileWriter(outputpath+vortex+"_join.ctl");
		fw.write(sb.toString());	fw.close();
	}
	
	private static void velticalInterpolation() throws IOException{
		// delete old files
		File fi=null;
		fi=new File(outputpath+vortex+".dat");	if(fi.exists()) fi.delete();
		fi=new File(outputpath+vortex+".ctl");	if(fi.exists()) fi.delete();
		
		// interpolation
		DataInterpolation di=new DataInterpolation(new CtlDescriptor(new File(outputpath+vortex+"_join.ctl")));
		di.verticalInterp(outputpath+vortex+".dat",37,"rh");
	}
	/**************************** data pre-process **********************************/
	
	
	/******************************* printing **************************************/
	private static void addVarible(String... vpro){
		ctlsb.append("'setlopts 7 0.24 300 100'\n");
		ctlsb.append("'set xaxis 0 "+dis+" "+interval+"'\n");
		
		if(vpro.length==3)
		ctlsb.append("'set cint "+vpro[2]+"'\n");
		
		ctlsb.append("'set clskip 2'\n");
		ctlsb.append("'set cthick 9'\n");
		ctlsb.append("'set grads off'\n");
		ctlsb.append("'set gxout contour'\n");
		ctlsb.append("'d "+vpro[0]+"'\n");
		ctlsb.append("'draw title "+vpro[1]+":'tim\n");
	}
	
	private static void addStart(String path,String zinfo){
		ctlsb.append("'enable print "+path+"'\n\n");
		ctlsb.append("tt=2\n");
		ctlsb.append("while(tt<"+csd.getTCount()+")\n");
		ctlsb.append("'set t 'tt\n");
		ctlsb.append("'q time'\n");
		ctlsb.append("res=subwrd(result,3)\n");
		ctlsb.append("tim=substr(res,1,8)\n");
		ctlsb.append("'set "+zinfo+"'\n");
		ctlsb.append("'set zlog on'\n");
	}
	
	private static void addEnd(){
		ctlsb.append("'print'\n");
		ctlsb.append("'c'\n");
		ctlsb.append("tt=tt+1\n");
		ctlsb.append("endwhile\n\n");
		ctlsb.append("'disable print'\n");
	}
	
	static void writeGS() throws IOException{
		System.out.println("Start writing gs file...");
		
		// create picture(pic) directory
		File fi=new File(outputpath+"pic");
		if(!fi.exists()) fi.mkdir();
		
		CtlDescriptor ctl=new CtlDescriptor(new File(outputpath+vortex+"_model.ctl"));
				
		dis=(float)Math.toRadians(csd.getDYDef()[0])*
			(csd.getYCount()-1)*SphericalSpatialModel.EARTH_RADIUS/1000;
		
		while(true){
			if((int)dis/interval<=11) break;
			
			interval+=50;
		}
		
		FileWriter fw=new FileWriter(outputpath+vortex+".gs");
		
		ctlsb.append("'open "+outputpath+vortex+"_model.ctl'\n");
		
		//
		ctlsb.append("******************* tangential variables *******************\n");
		addStart(outputpath+"pic/"+vortex+"_tan.gmf","z 1 "+ctl.getZCount());
		
		ctlsb.append("'setvpage 2 2 2 1'\n"); addVarible("ut" ,ctl.getVarComment("ut" ),"2"   );
		ctlsb.append("'setvpage 2 2 2 2'\n"); addVarible("vr" ,ctl.getVarComment("vr" ),"0.5" );
		ctlsb.append("'setvpage 2 2 1 1'\n"); addVarible("w"  ,ctl.getVarComment("w"  ),"0.05");
		ctlsb.append("'setvpage 2 2 1 2'\n"); addVarible("The",ctl.getVarComment("The")       );
		
		addEnd();
		/******************* tangential variables *******************/
		
		//
		ctlsb.append("******************* simulated result *******************\n");
		addStart(outputpath+"pic/"+vortex+"_sim.gmf","z 2 "+(ctl.getZCount()-1));
		
		ctlsb.append("'setvpage 2 2 2 1'\n"); addVarible("vsm","simulated "+ctl.getVarComment("vr").replace("force due to ",""),"0.5" );
		ctlsb.append("'setvpage 2 2 2 2'\n"); addVarible("wsm","simulated "+ctl.getVarComment("w" ).replace("force due to ",""),"0.05");
		ctlsb.append("'setvpage 2 2 1 1'\n"); addVarible("vr" ,"NCEP "+ctl.getVarComment("vr").replace("force due to ",""),"0.5" );
		ctlsb.append("'setvpage 2 2 1 2'\n"); addVarible("w"  ,"NCEP "+ctl.getVarComment("w" ).replace("force due to ",""),"0.05");
		
		addEnd();
		/******************* simulated result *******************/
		
		//
		ctlsb.append("******************* boundary effect *******************\n");
		addStart(outputpath+"pic/"+vortex+"_bou.gmf","z 2 "+(ctl.getZCount()-1));
		
		ctlsb.append("'setvpage 3 2 3 1'\n"); addVarible("vsm" ,ctl.getVarComment("vr").replace("force due to ","")+"(with boundary)"   ,"0.5" );
		ctlsb.append("'setvpage 3 2 3 2'\n"); addVarible("wsm" ,ctl.getVarComment("w" ).replace("force due to ","")+"(with boundary)"   ,"0.05");
		ctlsb.append("'setvpage 3 2 2 1'\n"); addVarible("vsfr",ctl.getVarComment("vr").replace("force due to ","")+"(without boundary)","0.5" );
		ctlsb.append("'setvpage 3 2 2 2'\n"); addVarible("wsfr",ctl.getVarComment("w" ).replace("force due to ","")+"(without boundary)","0.05");

		ctlsb.append("'setvpage 3 2 1 1'\n"); ctlsb.append("'set clab forced'\n");
		addVarible("vsbd",ctl.getVarComment("vr").replace("force due to ","")+"(boundary effect)");

		ctlsb.append("'setvpage 3 2 1 2'\n"); ctlsb.append("'set clab forced'\n");
		addVarible("wsbd",ctl.getVarComment("w" ).replace("force due to ","")+"(boundary effect)");
		
		addEnd();
		/******************* boundary effect *******************/
		
		//
		ctlsb.append("********************** radial wind **********************\n");
		addStart(outputpath+"pic/"+vortex+"_rad.gmf","z 2 "+(ctl.getZCount()-1));
		
		ctlsb.append("'setvpage 3 3 3 1'\n"); addVarible("vsfr",ctl.getVarComment("faf"      ).replace("force due to ",""));
		ctlsb.append("'setvpage 3 3 3 2'\n"); addVarible("vstf",ctl.getVarComment("fat"      ).replace("force due to ",""));
		ctlsb.append("'setvpage 3 3 3 3'\n"); addVarible("vsdf",ctl.getVarComment("fad"      ).replace("force due to ",""));
		ctlsb.append("'setvpage 3 3 2 1'\n"); addVarible("vs01",ctl.getVarComment("dhtFor"   ).replace("force due to ",""));
		ctlsb.append("'setvpage 3 3 2 2'\n"); addVarible("vs04",ctl.getVarComment("htHFCFor" ).replace("force due to ",""));
		ctlsb.append("'setvpage 3 3 2 3'\n"); addVarible("vs05",ctl.getVarComment("htVFCFor" ).replace("force due to ",""));
		ctlsb.append("'setvpage 3 3 1 1'\n"); addVarible("vs06",ctl.getVarComment("aamHFCFor").replace("force due to ",""));
		ctlsb.append("'setvpage 3 3 1 2'\n"); addVarible("vs07",ctl.getVarComment("aamVFCFor").replace("force due to ",""));
		ctlsb.append("'setvpage 3 3 1 3'\n"); addVarible("vs11",ctl.getVarComment("tiltFor"  ).replace("force due to ",""));
		
		addEnd();
		/********************** radial wind **********************/
		
		//
		ctlsb.append("********************** vertical velocity **********************\n");
		addStart(outputpath+"pic/"+vortex+"_ver.gmf","z 2 "+(ctl.getZCount()-1));
		
		ctlsb.append("'setvpage 3 3 3 1'\n"); addVarible("wsfr",ctl.getVarComment("faf"      ).replace("force due to ",""));
		ctlsb.append("'setvpage 3 3 3 2'\n"); addVarible("wstf",ctl.getVarComment("fat"      ).replace("force due to ",""));
		ctlsb.append("'setvpage 3 3 3 3'\n"); addVarible("wsdf",ctl.getVarComment("fad"      ).replace("force due to ",""));
		ctlsb.append("'setvpage 3 3 2 1'\n"); addVarible("ws01",ctl.getVarComment("dhtFor"   ).replace("force due to ",""));
		ctlsb.append("'setvpage 3 3 2 2'\n"); addVarible("ws04",ctl.getVarComment("htHFCFor" ).replace("force due to ",""));
		ctlsb.append("'setvpage 3 3 2 3'\n"); addVarible("ws05",ctl.getVarComment("htVFCFor" ).replace("force due to ",""));
		ctlsb.append("'setvpage 3 3 1 1'\n"); addVarible("ws06",ctl.getVarComment("aamHFCFor").replace("force due to ",""));
		ctlsb.append("'setvpage 3 3 1 2'\n"); addVarible("ws07",ctl.getVarComment("aamVFCFor").replace("force due to ",""));
		ctlsb.append("'setvpage 3 3 1 3'\n"); addVarible("ws11",ctl.getVarComment("tiltFor"  ).replace("force due to ",""));
		
		addEnd();
		/********************** vertical velocity **********************/
		
		
		//
		ctlsb.append("************************** force **************************\n");
		addStart(outputpath+"pic/"+vortex+"_for.gmf","z 2 "+(ctl.getZCount()-1));
		
		ctlsb.append("'setvpage 3 3 3 1'\n"); addVarible("faf"      ,ctl.getVarComment("faf"      ).replace("force due to ",""));
		ctlsb.append("'setvpage 3 3 3 2'\n"); addVarible("fat"      ,ctl.getVarComment("fat"      ).replace("force due to ",""));
		ctlsb.append("'setvpage 3 3 3 3'\n"); addVarible("fad"      ,ctl.getVarComment("fad"      ).replace("force due to ",""));
		ctlsb.append("'setvpage 3 3 2 1'\n"); addVarible("dhtFor"   ,ctl.getVarComment("dhtFor"   ).replace("force due to ",""));
		ctlsb.append("'setvpage 3 3 2 2'\n"); addVarible("htHFCFor" ,ctl.getVarComment("htHFCFor" ).replace("force due to ",""));
		ctlsb.append("'setvpage 3 3 2 3'\n"); addVarible("htVFCFor" ,ctl.getVarComment("htVFCFor" ).replace("force due to ",""));
		ctlsb.append("'setvpage 3 3 1 1'\n"); addVarible("aamHFCFor",ctl.getVarComment("aamHFCFor").replace("force due to ",""));
		ctlsb.append("'setvpage 3 3 1 2'\n"); addVarible("aamVFCFor",ctl.getVarComment("aamVFCFor").replace("force due to ",""));
		ctlsb.append("'setvpage 3 3 1 3'\n"); addVarible("tiltFor"  ,ctl.getVarComment("tiltFor"  ).replace("force due to ",""));
		
		addEnd();
		/************************** force **************************/
		
		//
		ctlsb.append("********************** surface variables **********************\n");
		addStart(outputpath+"pic/"+vortex+"_sur.gmf","lev 950 500");
		
		if(sfcFlux){
			ctlsb.append("'setvpage 3 3 3 1'\n"); addVarible("shrFor",ctl.getVarComment("shrFor").replace("force due to ",""));
			ctlsb.append("'setvpage 3 3 3 2'\n"); addVarible("lhrFor",ctl.getVarComment("lhrFor").replace("force due to ",""));
			ctlsb.append("'setvpage 3 3 3 3'\n"); addVarible("fsfcXL",ctl.getVarComment("fsfcXL").replace("force due to ",""));
			ctlsb.append("'setvpage 3 3 2 1'\n"); addVarible("vs02"  ,ctl.getVarComment("shrFor").replace("force due to ",""));
			ctlsb.append("'setvpage 3 3 2 2'\n"); addVarible("vs03"  ,ctl.getVarComment("lhrFor").replace("force due to ",""));
			ctlsb.append("'setvpage 3 3 2 3'\n"); addVarible("vs08"  ,ctl.getVarComment("fsfFor").replace("force due to ",""));
			ctlsb.append("'setvpage 3 3 1 1'\n"); addVarible("ws02"  ,ctl.getVarComment("shrFor").replace("force due to ",""));
			ctlsb.append("'setvpage 3 3 1 2'\n"); addVarible("ws03"  ,ctl.getVarComment("lhrFor").replace("force due to ",""));
			ctlsb.append("'setvpage 3 3 1 3'\n"); addVarible("ws08"  ,ctl.getVarComment("fsfFor").replace("force due to ",""));
			
		}else{
			ctlsb.append("'setvpage 3 3 3 3'\n"); addVarible("fsfcXL",ctl.getVarComment("fsfcXL").replace("force due to ",""));
			ctlsb.append("'setvpage 3 3 2 1'\n"); addVarible("vs02"  ,"sensible heat flux");
			ctlsb.append("'setvpage 3 3 2 2'\n"); addVarible("vs03"  ,"latent heat flux");
			ctlsb.append("'setvpage 3 3 2 3'\n"); addVarible("vs08"  ,ctl.getVarComment("fsfFor").replace("force due to ",""));
			ctlsb.append("'setvpage 3 3 1 1'\n"); addVarible("ws02"  ,"sensible heat flux");
			ctlsb.append("'setvpage 3 3 1 2'\n"); addVarible("ws03"  ,"latent heat flux");
			ctlsb.append("'setvpage 3 3 1 3'\n"); addVarible("ws08"  ,ctl.getVarComment("fsfFor").replace("force due to ",""));
		}
		
		addEnd();
		/********************** surface variables **********************/
		
		ctlsb.append("*************************************************************\n");
		ctlsb.append("'reinit'\n");
		ctlsb.append("'quit'\n");
		
		fw.write(ctlsb.toString());
		fw.close();
		
		System.out.println("Finish writing gs.");
	}
	/******************************* printing **************************************/
	
	
	//
	public static void main(String[] args){
		ConcurrentUtil.initDefaultExecutor(3);
		try{
			//dataPreProcess();
			modeling();
			writeGS();
			printing();
			
	    }catch(Exception ex){ ex.printStackTrace(); System.exit(0);}
		ConcurrentUtil.shutdown();
	}
}
