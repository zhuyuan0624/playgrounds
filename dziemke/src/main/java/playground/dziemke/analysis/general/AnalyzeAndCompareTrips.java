package playground.dziemke.analysis.general;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import playground.dziemke.analysis.GnuplotUtils;
import playground.dziemke.analysis.general.matsim.Events2TripsParser;
import playground.dziemke.analysis.general.matsim.FromMatsimTrip;
import playground.dziemke.analysis.general.matsim.MatsimTripFilterImpl;
import playground.dziemke.analysis.general.srv.FromSrvTrip;
import playground.dziemke.analysis.general.srv.SrvTripFilterImpl;
import playground.dziemke.analysis.general.srv.Srv2MATSimPopulation;

import java.io.File;
import java.util.List;

/**
 * @author gthunig, dziemke
 */
public class AnalyzeAndCompareTrips {
	public static final Logger log = Logger.getLogger(AnalyzeAndCompareTrips.class);

	// Parameters
	private static final String RUN_ID = "be_255"; // <----------
	private static final String ITERATION_FOR_ANALYSIS = ""; // use empty string is not used
//	private static final String CEMDAP_PERSONS_INPUT_FILE_ID = "21"; // Check if this number corresponds correctly to the RUN_ID

	// Input and output
//	private static final String NETWORK_FILE = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/network_counts/network.xml.gz"; // <----------
	private static final String NETWORK_FILE = "../../shared-svn/studies/countries/de/berlin_scenario_2016/network_counts/network_shortIds.xml.gz"; // <----------
	private static final String CONFIG_FILE = "../../runs-svn/berlin_scenario_2016/" + RUN_ID + "/" + RUN_ID + ".output_config.xml.gz";
//	private static final String CONFIG_FILE = "../../runs-svn/berlin_scenario_2016/" + RUN_ID + "/config_be_202.xml";
	private static final String EVENTS_FILE = "../../runs-svn/berlin_scenario_2016/" + RUN_ID + "/" + RUN_ID + ".output_events.xml.gz";
//	private static final String EVENTS_FILE = "../../runs-svn/berlin_scenario_2016/" + RUN_ID + "/ITERS/it." + ITERATION_FOR_ANALYSIS + "/" + RUN_ID + "." + ITERATION_FOR_ANALYSIS + ".events.xml.gz";
//	private static final String cemdapPersonsInputFile = "../../shared-svn/projects/cemdapMatsimCadyts/scenario/cemdap_berlin/" + CEMDAP_PERSONS_INPUT_FILE_ID + "/persons1.dat"; // TODO
	private static final String AREA_SHAPE_FILE = "../../shared-svn/studies/countries/de/berlin_scenario_2016/input/shapefiles/2013/Berlin_DHDN_GK4.shp";
//	private static String outputDirectory = "../../../runs-svn/berlin_scenario_2016/" + RUN_ID + "/analysis";
	private static String analysisOutputDirectory = "../../runs-svn/berlin_scenario_2016/" + RUN_ID + "/analysis_run";

	//FromSrv Parameters
	private static final String SRV_BASE_DIR = "../../shared-svn/studies/countries/de/berlin_scenario_2016/analysis/srv/input/";
	private static final String SRV_PERSON_FILE_PATH = SRV_BASE_DIR + "P2008_Berlin2.dat";
	private static final String SRV_TRIP_FILE_PATH = SRV_BASE_DIR + "W2008_Berlin_Weekday.dat";
	private static final String OUTPUT_POPULATION_FILE_PATH = SRV_BASE_DIR + "testOutputPopulation.xml";
//	private static String fromSrvOutputDirectory = "/Users/dominik/test-analysis";
//	private static String fromSrvOutputDirectory = "../../../runs-svn/berlin_scenario_2016/" + RUN_ID + "/analysis_srv";
	private static String fromSrvOutputDirectory = "";


	public static void main(String[] args) {

		Events2TripsParser events2TripsParser = new Events2TripsParser(CONFIG_FILE, EVENTS_FILE, NETWORK_FILE);
		List<FromMatsimTrip> fromMatsimTrips = events2TripsParser.getTrips();

		MatsimTripFilterImpl matsimTripFilter = new MatsimTripFilterImpl();
//		matsimTripFilter.activateModeChoice(TransportMode.car);
//		matsimTripFilter.activateModeChoice("pt", "ptSlow");
//		matsimTripFilter.activateModeChoice("ptSlow");
//		matsimTripFilter.activateModeChoice("bicycle");
		matsimTripFilter.activateStartsOrEndsIn(events2TripsParser.getNetwork(), AREA_SHAPE_FILE, 11000000);
		matsimTripFilter.activateDist(0, 100);
//		matsimTripFilter.activateDepartureTimeRange(7. * 3600, 9. * 3600);
//		matsimTripFilter.activateDepartureTimeRange(16. * 3600, 22. * 3600);
		List<Trip> filteredFromMatsimTrips = TripFilter.castTrips(matsimTripFilter.filter(fromMatsimTrips));

		// Determine output directory
		if (!ITERATION_FOR_ANALYSIS.equals("")) {
			if (EVENTS_FILE.contains(".output_events.xml.gz")) {
				throw new RuntimeException("Specifying a specific iteration for the analysis in the output is confusing if general output events are used.");
			}
			analysisOutputDirectory = analysisOutputDirectory + "_" + ITERATION_FOR_ANALYSIS;
		}
		analysisOutputDirectory = matsimTripFilter.adaptOutputDirectory(analysisOutputDirectory);
		new File(analysisOutputDirectory).mkdirs();

		// Write output
		GeneralTripAnalyzer.analyze(filteredFromMatsimTrips, events2TripsParser.getNoPreviousEndOfActivityCounter(), analysisOutputDirectory);


		Srv2MATSimPopulation srv2MATSimPopulation = new Srv2MATSimPopulation(SRV_PERSON_FILE_PATH, SRV_TRIP_FILE_PATH);
		srv2MATSimPopulation.writePopulation(OUTPUT_POPULATION_FILE_PATH);

		List<FromSrvTrip> fromSrvTrips = srv2MATSimPopulation.getTrips();

		SrvTripFilterImpl srvTripFilter = new SrvTripFilterImpl();
//		srvTripFilter.activateModeChoice(TransportMode.bike);
		srvTripFilter.activateDist(0, 100);
//		srvTripFilter.activateDepartureTimeRange(7. * 3600, 9. * 3600);
//		srvTripFilter.activateDepartureTimeRange(16. * 3600, 22. * 3600);

		// Determine output directory
		String srvOutputDirectory = srvTripFilter.adaptOutputDirectory("analysis_srv");
		fromSrvOutputDirectory = analysisOutputDirectory + "/" + srvOutputDirectory;
		new File(fromSrvOutputDirectory).mkdirs();

		if (!GeneralTripAnalyzer.doesExist(fromSrvOutputDirectory)) {
			//filter
			List<Trip> filteredFromSrvTrips = TripFilter.castTrips(srvTripFilter.filter(fromSrvTrips));
			//write output
			GeneralTripAnalyzer.analyze(filteredFromSrvTrips, fromSrvOutputDirectory);
		}

		//Gnuplot
		String gnuplotScriptName = "plot_abs_path_run.gnu";
		String relativePathToGnuplotScript = "../../../../shared-svn/studies/countries/de/berlin_scenario_2016/analysis/gnuplot/" + gnuplotScriptName;
		GnuplotUtils.runGnuplotScript(analysisOutputDirectory, relativePathToGnuplotScript, srvOutputDirectory);

		deleteFolder(new File(fromSrvOutputDirectory));
	}

	private static void deleteFolder(File folder) {
		File[] files = folder.listFiles();
		if(files!=null) {
			for(File f: files) {
				if(f.isDirectory()) {
					deleteFolder(f);
				} else {
					f.delete();
				}
			}
		}
		folder.delete();
	}
}