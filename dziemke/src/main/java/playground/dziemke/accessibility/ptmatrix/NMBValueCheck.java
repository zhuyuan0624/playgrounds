package playground.dziemke.accessibility.ptmatrix;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import playground.dziemke.accessibility.ptmatrix.TransitLeastCostPathRouting.TransitRouterImpl;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author gabriel
 * on 19.04.16.
 */
public class NMBValueCheck {

    public static void main(String[] args) {

        File file = new File("");
        try {
            System.out.println(file.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        String networkFile = "playgrounds/dziemke/input/NMBM_PT_V1.xml";
        String transitScheduleFile = "playgrounds/dziemke/input/Transitschedule_PT_V1_WithVehicles.xml";

        Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup(), new MatrixBasedPtRouterConfigGroup());
        config.network().setInputFile(networkFile);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        scenario.getConfig().transit().setUseTransit(true);

        // Read in public transport schedule
        TransitScheduleReader reader = new TransitScheduleReader(scenario);
        reader.readFile(transitScheduleFile);
        TransitSchedule transitSchedule = scenario.getTransitSchedule();

        // constructor of TransitRouterImpl needs TransitRouterConfig. This is why it is instantiated here.
        TransitRouterConfig transitRouterConfig = new TransitRouterConfig(scenario.getConfig());
        TransitRouter transitRouter = new TransitRouterImpl(transitRouterConfig, transitSchedule);

        Double departureTime = 8. * 60 * 60;
        Coord coord1 = new Coord(129721.15152725934,-3690834.0129102874);
        Coord coord2 = new Coord(147819.75957863466,-3699365.4230317925);
        Coord coord3 = new Coord(137547.07266149623,-3706738.5909946687);
        Coord coord4 = new Coord(140245.15520623303,-3693657.6437037485);
        Coord coord5 = new Coord(149770.37397993292,-3689099.1898143673);

        CoordinateTransformation coordinateTransformation = TransformationFactory.
                getCoordinateTransformation(TransformationFactory.WGS84_SA_Albers, TransformationFactory.WGS84);
        Coord inverseCoord = new Coord(coordinateTransformation.transform(coord3).getY(), coordinateTransformation.transform(coord3).getX());
        System.out.println("coord3 = " + inverseCoord);
        inverseCoord = new Coord(coordinateTransformation.transform(coord4).getY(), coordinateTransformation.transform(coord4).getX());
        System.out.println("coord4 = " + inverseCoord);

        List<Leg> legList = transitRouter.calcRoute(coord3, coord4, departureTime, null);

        double travelTime = 0.;
        double travelDistance = 0.;
        for (Leg leg : legList) {
            if(leg == null) {
                throw new RuntimeException("Leg is null.");
            }
            travelTime += leg.getTravelTime();
            System.out.println("travelTime = " + travelTime);
            String mode = leg.getMode();
            System.out.println("mode = " + mode);
            Route legRoute = leg.getRoute();
            if (legRoute != null) {
                System.out.println("Route description: " + legRoute.getRouteDescription());
                System.out.println("Route start link id: " + legRoute.getStartLinkId());
                if (legRoute.getStartLinkId() != null) {
                    Coord startLinkCoord = scenario.getNetwork().getLinks().get(legRoute.getStartLinkId()).getFromNode().getCoord();
                    System.out.println("Route start link coord"
                            + new Coord(coordinateTransformation.transform(startLinkCoord).getY(),
                            coordinateTransformation.transform(startLinkCoord).getX()));
                }
                System.out.println("Route end link id: " + legRoute.getEndLinkId());
                if (legRoute.getEndLinkId() != null) {
                    Coord endLinkCoord = scenario.getNetwork().getLinks().get(legRoute.getEndLinkId()).getToNode().getCoord();
                    System.out.println("Route start link coord"
                            + new Coord(coordinateTransformation.transform(endLinkCoord).getY(),
                            coordinateTransformation.transform(endLinkCoord).getX()));
                }
            } else {
                System.out.println("LegRoute == null");
            }
        }
        System.out.println("final travelTime = " + travelTime);
    }

}
