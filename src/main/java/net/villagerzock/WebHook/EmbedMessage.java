package net.villagerzock.WebHook;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class EmbedMessage implements Message {
    protected final JsonObject object = new JsonObject();
    protected final String content;
    protected final List<Embed> embeds;
    public EmbedMessage(String content,List<Embed> embeds){
        this.content = content;
        this.embeds = embeds;
        generateObject();
    }
    protected void generateObject(){
        //System.out.println(content);
        if (content != ""){
            object.addProperty("content",content);
        }
        JsonArray array = new JsonArray();
        for (int i = 0; i<embeds.size();i++){
            array.add(embeds.get(i).Serialize());
        }
        object.add("embeds",array);
    }

    @Override
    public String getMessage() {
        return object.toString();
    }

    public static class Builder{
        protected String content = "";
        protected List<Embed> embeds = new ArrayList<>();
        public Builder setContent(String content){
            this.content = content;
            return this;
        }
        public Builder addEmbed(Embed embed){
            embeds.add(embed);
            return this;
        }
        public EmbedMessage build(){
            return new EmbedMessage(content,embeds);
        }
    }
}
