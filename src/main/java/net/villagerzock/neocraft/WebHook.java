package net.villagerzock.neocraft;

import net.villagerzock.WebHook.Message;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

public class WebHook {
    protected final URL url;
    public Registries registries;
    protected String token  = "";
    protected String name = "";
    public WebHook(String url) throws IOException {
        this.url = new URL(url);
        registries = new Registries();
    }
    public WebHook(String url,String token,String name) throws IOException {
        this.url = new URL(url);
        this.token = token;
        this.name = name;
        registries = new Registries();
    }
    public void sendMessage(Message message) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.addRequestProperty("Content-Type","application/json");
        connection.addRequestProperty("User-Agent","Java-DiscordWebhook");
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");

        OutputStream stream = connection.getOutputStream();
        String toSend = message.getMessage();
        stream.write(toSend.getBytes());
        stream.flush();
        stream.close();

        connection.getInputStream().close();
        connection.disconnect();
    }
}

