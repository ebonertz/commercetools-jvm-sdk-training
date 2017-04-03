package handson;

import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.client.SphereClientFactory;
import javafx.scene.shape.Sphere;

import java.io.IOException;
import java.time.Duration;
import java.util.Properties;

class Utils {

    /**
     * Creates a blocking sphere client
     * @return Sphere client
     */
    static BlockingSphereClient createSphereClient() throws IOException{

        //3.1.1. Get the project info from the Admin Center
        //3.1.2. Create the configuration for the sphere client
        //3.1.3. Create the client and return it
        final String projectKey = "rhtraining";
        final String clientId = "ry-j33MjdectKkrIptZeJI3h";
        final String clientSecret = "Wg9uKUEp3_M_VVNFMjGPKg9JVAg5N603";
        final String authUrl = "https://auth.commercetools.co";
        final String apiUrl = "https://api.commercetools.co";


        final SphereClientConfig clientConfig2 = SphereClientConfig.of(projectKey, clientId, clientSecret, authUrl, apiUrl);
        return BlockingSphereClient.of(SphereClientFactory.of().createClient(clientConfig2), Duration.ofMinutes(1));
    }
}