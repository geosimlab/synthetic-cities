package syncity.network;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.util.Collection;

public class ExampleShapeReader {

    public static void main(String[] args) {

        String shapeFile = "E:\\Files\\dumps\\Guardrails.shp";


        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);

        for(SimpleFeature feature: features) {
        	System.out.println(feature.getName() + " " + feature.getDescriptor() + " " + feature.getID());
            feature.getProperties().stream()
                        .filter(prty -> prty != null)
                        .forEach(prty -> System.out.println("\t"+prty.getName() + " : " + prty.getDescriptor()+
                                                            "\n\t\t"+prty.getValue()));
            break;
        }

    }
}
