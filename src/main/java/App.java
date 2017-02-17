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

	private WebView authWebView;
	private String authCode;
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

		authWebView = new WebView("VK authorization", 655, 372);
	}

	public static void main(String[] args) {
		new App().start();
	}

	public void start() {
		if (!readTokenFromFile()) {
			authorize();
		}
//		initAPIClient();
		process();
	}

	private void authorize() {
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

		// non-blocking operation
//		new WebView(this, "VK authorization", 640, 480).openURL(builder.toString());
		
		// blocking operation
		String newLocation = authWebView.openURL(builder.toString());
		System.out.println("New location from authorize: " + newLocation);
		authCode = extractCode(newLocation);
		
		TransportClient transportClient = HttpTransportClient.getInstance();
		vk = new VkApiClient(transportClient);
		actor = new UserActor(userId, token);
		
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
	}

	public void onWebViewClosed() {
		String url = authWebView.getURL();
		authCode = extractCode(url);

		
		
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
		saveTokenToFile();

		initAPIClient();
	}

	private void initAPIClient() {
		
		
//		process();
	}

	private void process() {
		// if (readTokenFromFile()) {
		// TransportClient transportClient = HttpTransportClient.getInstance();
		// vk = new VkApiClient(transportClient);
		// }

		// actor = new UserActor(userId, token);

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

	private String extractCode(String url) {
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

	private void saveTokenToFile() {
		try {
			PrintWriter writer = new PrintWriter(tokenFileName, "UTF-8");
			writer.println(userId);
			writer.print(token);
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	private boolean readTokenFromFile() {
		File file = new File(tokenFileName);
		if (file.exists() && !file.isDirectory()) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(tokenFileName));
				userId = Integer.parseInt(br.readLine());
				token = br.readLine();
				br.close();
				return true;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		} else {
			return false;
		}
	}

}
