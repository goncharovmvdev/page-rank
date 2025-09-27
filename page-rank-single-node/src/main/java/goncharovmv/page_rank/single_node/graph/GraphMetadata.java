package goncharovmv.page_rank.single_node.graph;

import java.util.Map;
import java.util.Set;

public interface GraphMetadata {

    Map<Integer, Integer> outgoingDegrees();

    default Set<Integer> vertices() {
        return outgoingDegrees().keySet();
    }
}
