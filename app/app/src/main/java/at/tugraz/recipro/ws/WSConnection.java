package at.tugraz.recipro.ws;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Looper;
import android.util.Log;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import at.tugraz.recipro.data.Allergen;
import at.tugraz.recipro.data.Ingredient;
import at.tugraz.recipro.data.Recipe;
import at.tugraz.recipro.helper.ResourceAccessHelper;
import at.tugraz.recipro.recipro.R;

public class WSConnection {

    private static WSConnection instance;

    private WSConnection() {
    }

    public void init(String uri, boolean testhandling) {
        this.backend_uri = uri;
        this.testhandling = testhandling;
    }

    public static WSConnection getInstance() {
        if (instance == null)
            instance = new WSConnection();
        return instance;
    }

    private String backend_uri = "https://aughrany.com/recipro-backend/api";
    private String backend_path_recipes = "/recipes";
    private String backend_path_image = "/recipes/%d/image";
    private String backend_path_ingredients = "/recipes/ingredients";
    private String backend_path_allergens = "/allergens";
    private boolean testhandling = false;

    private static final String LOG_TAG = WSConnection.class.getName();

    private void showAlertDialog() {
        if(!testhandling) {
            Looper.prepare();

            Context context = ResourceAccessHelper.getAppContext();
            AlertDialog.Builder builder;
            builder = new AlertDialog.Builder(context);
            builder.setTitle(ResourceAccessHelper.getStringFromId(R.string.error_connect))
                    .setMessage(ResourceAccessHelper.getStringFromId(R.string.cannot_connect_message))
                    .setNegativeButton(R.string.close, (dialog, which) -> System.exit(0))
                    .setIcon(android.R.drawable.ic_dialog_alert);
            builder.show();

            while (true) {
                Looper.loop();
            }
        }
    }

    private ResponseEntity getRequest(String path, MultiValueMap<String, String> queryParams, Class clazz) {
        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(backend_uri).path(path);

            if (queryParams != null)
                uriBuilder.queryParams(queryParams);

            String uri = uriBuilder.build().toString();
            Log.d(LOG_TAG, "request_uri=" + uri);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            RestTemplate restTemplate = new RestTemplate();
            restTemplate.getMessageConverters().add(new GsonHttpMessageConverter());
            return restTemplate.exchange(uri, HttpMethod.GET, entity, clazz);
        } catch(RestClientException ex) {
            Log.e(getClass().getSimpleName(), "Could not connect to backend... bye bye");
            showAlertDialog();
            System.exit(1);
            return null;
        }
    }

    public List<Ingredient> requestIngredients() throws RestClientException {
        ResponseEntity<Ingredient[]> response = getRequest(backend_path_ingredients, null, Ingredient[].class);
        Log.i(LOG_TAG, "status=" + response.getStatusCode() + " length=" + response.getBody().length);
        return Arrays.asList(response.getBody());
    }

    public List<Allergen> requestAllergens() throws RestClientException {
            ResponseEntity<Allergen[]> response = getRequest(backend_path_allergens, null, Allergen[].class);
        Log.i(LOG_TAG, "status=" + response.getStatusCode() + " length=" + response.getBody().length);
        return Arrays.asList(response.getBody());
    }

    public List<Recipe> requestRecipes(MultiValueMap<String, String> queryParams) throws RestClientException {
        ResponseEntity<Recipe[]> response = getRequest(backend_path_recipes, queryParams, Recipe[].class);
        Log.i(LOG_TAG, "status=" + response.getStatusCode() + " length=" + response.getBody().length);
        return Arrays.asList(response.getBody());
    }

    public boolean postImage(long recipeId, byte[] image, ImageType imageType) {
        try {
            String path = String.format(backend_path_image, recipeId);
            String uri = UriComponentsBuilder.fromHttpUrl(backend_uri).path(path).build().toString();

            Log.d(LOG_TAG, "post_image_uri=" + uri);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(imageType == ImageType.JPEG ? MediaType.IMAGE_JPEG : MediaType.IMAGE_PNG);
            HttpEntity<byte[]> entity = new HttpEntity<>(image, headers);

            RestTemplate restTemplate = new RestTemplate();
            restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter());

            ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.POST, entity, Void.class);

            Log.d(LOG_TAG, "status=" + response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.CREATED) {
                Log.d(LOG_TAG, "location_uri=" + response.getHeaders().getFirst(WSConstants.HTTP_LOCATION_HEADER));
                return true;
            }
           return false;
        } catch(RestClientException ex) {
            Log.e(getClass().getSimpleName(), "Could not connect to backend...");
            showAlertDialog();
            System.exit(1);
            return false;
        }
    }

    public Bitmap getImage(long recipeId) {
        try {
            String path = String.format(backend_path_image, recipeId);

            String uri = UriComponentsBuilder.fromHttpUrl(backend_uri).path(path).build().toString();

            Log.d(LOG_TAG, "get_image_uri=" + uri);

            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            RestTemplate restTemplate = new RestTemplate();
            restTemplate.getMessageConverters().add(new ResourceHttpMessageConverter());

            try {
                ResponseEntity<Resource> response = restTemplate.exchange(uri, HttpMethod.GET, entity, Resource.class);
                Log.d(LOG_TAG, "status=" + response.getStatusCode());

                InputStream responseInputStream;
                responseInputStream = response.getBody().getInputStream();

                return BitmapFactory.decodeStream(responseInputStream);

            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                    Log.d(LOG_TAG, "image not found");
                    return null;
                }
                throw ex;
            } catch (IOException ex) {
                Log.d(LOG_TAG, "could not decode image");
                return null;
            }
        } catch(HttpClientErrorException ex) {
            throw ex;
        } catch(RestClientException ex) {
            Log.e(getClass().getSimpleName(), "Could not connect to backend...");
            showAlertDialog();
            System.exit(1);
            return null;
        }
    }
}
