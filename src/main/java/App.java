import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.UserAuthResponse;
import com.vk.api.sdk.objects.friends.UserXtrLists;
import com.vk.api.sdk.queries.users.UserField;

public class App {

	private int appID;
	private String clientSecret;
	private String redirectURI;
	private String tokenFileName;

	private String token;
	private int userId;
	private VkApiClient vk;
	private UserActor actor;

	public App() {
		try {
			FileInputStream fis = new FileInputStream("src/main/resources/config.properties");
			Properties properties = new Properties();
			properties.load(fis);
			appID = Integer.parseInt(properties.getProperty("vk.api.app_id"));
			clientSecret = properties.getProperty("vk.api.client_secret");
			redirectURI = properties.getProperty("vk.api.redirect_uri");
			tokenFileName = properties.getProperty("vk.api.token_file");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

//		authWebView = new VkAuthWebView("VK authorization", 655, 372);
	}

	public static void main(String[] args) {
		new App().start();
	}

	public void start() {
		TransportClient transportClient = HttpTransportClient.getInstance();
		vk = new VkApiClient(transportClient);
		Credentials credentials = authorize();
		actor = new UserActor(credentials.getUserId(), credentials.getToken());
		process();
	}

	private Credentials authorize() {
		Credentials credentials = readCredentialsFromFile();
		if(credentials == null) {
			// using web view for authorization
			/*
			 * https://oauth.vk.com/authorize?client_id=5490057 &display=page
			 * &redirect_uri=https://oauth.vk.com/blank.html &scope=friends
			 * &response_type=token &v=5.52
			 */
			StringBuilder builder = new StringBuilder();
			builder.append("https://oauth.vk.com/authorize?client_id=").append(appID).append("&display=").append("page")
					.append("&redirect_uri=").append(redirectURI).append("&scope=").append("friends")
					.append("&response_type=").append("code").append("&v=").append("5.62");

			// blocking operation
			String newURL = new VkAuthWebView("VK authorization", 655, 372).openURL(builder.toString());
			System.out.println("New location from authorize: " + newURL);
			String authCode = extractAuthCodeFromURL(newURL);
			
			UserAuthResponse authResponse = null;
			try {
				authResponse = vk.oauth().userAuthorizationCodeFlow(appID, clientSecret, redirectURI, authCode).execute();
			} catch (ApiException e) {
				e.printStackTrace();
			} catch (ClientException e) {
				e.printStackTrace();
			}
			userId = authResponse.getUserId();
			token = authResponse.getAccessToken();
			credentials = new Credentials(userId, token);
			saveCredentialsToFile(credentials);
			return credentials;
		} else {
			return readCredentialsFromFile();
		}
	}

	private void process() {
		try {
			List<UserXtrLists> friends = vk.friends().get(actor, UserField.SCREEN_NAME, UserField.SEX).execute()
					.getItems();
			for (UserXtrLists user : friends) {
				StringBuilder builder = new StringBuilder();
				builder.append(user.getScreenName()).append(" - ").append(user.getSex().name());
				System.out.println(builder.toString());
			}

		} catch (ApiException e) {
			e.printStackTrace();
		} catch (ClientException e) {
			e.printStackTrace();
		}

	}

	private String extractAuthCodeFromURL(String url) {
		String regex = "code=[a-z0-9]*";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(url);
		String result = "";
		if (matcher.find()) {
			result = matcher.group(0);
			StringTokenizer st = new StringTokenizer(result, "=");
			if (st.hasMoreTokens()) {
				st.nextToken();
				result = st.nextToken();
			}
		}
		return result;
	}

	private void saveCredentialsToFile(Credentials credentials) {
		try {
			PrintWriter writer = new PrintWriter(tokenFileName, "UTF-8");
			writer.println(credentials.getUserId());
			writer.print(credentials.getToken());
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	private Credentials readCredentialsFromFile() {
		File file = new File(tokenFileName);
		if (file.exists() && !file.isDirectory()) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(tokenFileName));
				userId = Integer.parseInt(br.readLine());
				token = br.readLine();
				Credentials credentials = new Credentials(userId, token);
				br.close();
				return credentials;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			return null;
		}
	}

}
