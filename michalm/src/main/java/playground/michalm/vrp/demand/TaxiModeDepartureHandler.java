package playground.michalm.vrp.demand;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.core.mobsim.framework.*;
import org.matsim.ptproject.qsim.interfaces.*;

import pl.poznan.put.vrp.dynamic.data.model.*;
import playground.michalm.vrp.data.*;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.sim.*;


public class TaxiModeDepartureHandler
    implements DepartureHandler
{
    private static final String TAXI_MODE = "taxi";

    private MATSimVRPData data;
    private VRPSimEngine vrpEngine;


    public TaxiModeDepartureHandler(VRPSimEngine vrpEngine, MATSimVRPData data)
    {
        this.vrpEngine = vrpEngine;
        this.data = data;
    }


    @Override
    public boolean handleDeparture(double now, MobsimAgent agent, Id linkId)
    {
        if (agent.getMode().equals(TAXI_MODE)) {
            // mobsim.getEventsManager().processEvent(
            // new TaxiRequestEventImpl(now, agent.getId(), linkId, agent.getMode()));

            MATSimVRPGraph vrpGraph = data.getVrpGraph();

            MATSimVertex fromVertex = vrpGraph.getVertex(linkId);

            Id toLinkId = agent.getDestinationLinkId();
            MATSimVertex toVertex = vrpGraph.getVertex(toLinkId);

            List<Customer> customers = data.getVrpData().getCustomers();
            List<Request> requests = data.getVrpData().getRequests();

            int id = requests.size();

            // notify the DVRP Optimizer
            // agent -> customerId -> Customer
            Customer customer = new TaxiCustomer(id, fromVertex, agent);// TODO

            int duration = 120; // approx. 120 s for entering the taxi
            int t0 = (int)now;
            int t1 = t0 + 3600; // hardcoded values!
            Request request = new RequestImpl(id, customer, fromVertex, toVertex, 1, 1, duration,
                    t0, t1, false);

            customers.add(customer);
            requests.add(request);

            vrpEngine.getMobsim().registerAdditionalAgentOnLink(agent);
            vrpEngine.taxiRequestSubmitted(request, now);

            return true;
        }
        else {
            return false;
        }
    }

}
