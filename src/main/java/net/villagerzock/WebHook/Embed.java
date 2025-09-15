package net.villagerzock.WebHook;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;

public class Embed {
    public static final String RED = "FF0000";
    public static final String GREEN = "00FF00";
    public static final String BLUE = "0000FF";
    public static final String YELLOW = "FFD800";
    public static final String PINK = "B200FF";
    public static final String MAGENTA = "FF00DC";
    public static final String BROWN = "771B00";
    public static final String BLACK = "000000";
    public static final String WHITE = "FFFFFF";
    protected final String Author;
    protected final String AuthorURL;
    protected final String AuthorIconURL;
    protected final String BodyTitle;
    protected final String BodyDescription;
    protected final String BodyURL;
    protected final String BodyColor;
    protected final List<Field> Fields;
    protected final String ImageURL;
    protected final String ThumbnailURL;
    protected final String Footer;
    protected final ZonedDateTime FooterTimeStamp;
    protected final String FooterIconURL;
    public JsonObject Serialize(){
        JsonObject object = new JsonObject();
        if (BodyTitle != "")
            object.addProperty("title",BodyTitle);
        if (BodyDescription != "")
            object.addProperty("description",BodyDescription);
        if (BodyColor != "")
            object.addProperty("color",Integer.parseInt(BodyColor,16));
        if (BodyURL != "")
            object.addProperty("url",BodyURL);
        if (Fields.size() > 0){
            JsonArray fields = new JsonArray();
            for (int i = 0; i < Fields.size(); i++) {
                fields.add(Fields.get(i).serialize());
            }
            object.add("fields", fields);
        }

        JsonObject author = new JsonObject();
        if (Author != "")
            author.addProperty("name",Author);
        if (AuthorURL != "")
            author.addProperty("url",AuthorURL);
        if (AuthorIconURL != "")
            author.addProperty("icon_url",AuthorIconURL);
        if (author.size() > 0)
            object.add("author",author);

        JsonObject footer = new JsonObject();
        if (Footer != "")
            footer.addProperty("text",Footer);
        if (FooterIconURL != "")
            footer.addProperty("url",FooterIconURL);
        if (footer.size() > 0)
            object.add("footer",footer);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

        object.addProperty("timestamp", FooterTimeStamp.format(formatter));

        JsonObject Image = new JsonObject();
        if (ImageURL != "")
            Image.addProperty("url",ImageURL);
        if (Image.size() > 0)
            object.add("image",Image);

        JsonObject thumbnail = new JsonObject();
        if (ThumbnailURL != "")
            thumbnail.addProperty("url",ThumbnailURL);
        if (thumbnail.size() > 0)
            object.add("thumbnail",thumbnail);
        return object;
    }
    public Embed(String author, String authorURL, String authorIconURL, String bodyTitle, String bodyDescription, String bodyURL, String bodyColor, List<Field> fields, String imageURL, String thumbnailURL, String footer, ZonedDateTime footerTimeStamp, String footerIconURL) {
        Author = author;
        AuthorURL = authorURL;
        AuthorIconURL = authorIconURL;
        BodyTitle = bodyTitle;
        BodyDescription = bodyDescription;
        BodyURL = bodyURL;
        BodyColor = bodyColor;
        Fields = fields;
        ImageURL = imageURL;
        ThumbnailURL = thumbnailURL;
        Footer = footer;
        FooterTimeStamp = footerTimeStamp;
        FooterIconURL = footerIconURL;
    }
    public static class Builder{
        protected String Author = "";
        protected String AuthorURL = "";
        protected String AuthorIconURL = "";
        protected String BodyTitle = "";
        protected String BodyDescription = "";
        protected String BodyURL = "";
        protected String BodyColor = "";
        protected List<Field> Fields = new ArrayList<>();
        protected String ImageURL = "";
        protected String ThumbnailURL = "";
        protected String Footer = "";
        protected ZonedDateTime FooterTimeStamp = ZonedDateTime.now();
        protected String FooterIconURL = "";
        public Builder(){

        }
        public Builder setAuthor(String name,String url, String iconUrl){
            this.Author = name;
            this.AuthorURL = url;
            this.AuthorIconURL = iconUrl;
            return this;
        }
        public Builder setBody(String title,String desc,String url, String color){
            this.BodyTitle = title;
            this.BodyDescription = desc;
            this.BodyURL = url;
            this.BodyColor = color;
            return this;
        }
        public Builder addField(Field field){
            this.Fields.add(field);
            return this;
        }
        public Builder setImage(String url){
            this.ImageURL = url;
            return this;
        }
        public Builder setThumbnail(String url){
            this.ThumbnailURL = url;
            return this;
        }
        public Builder setFooter(String footer,ZonedDateTime Timestamp, String footerIconURL){
            this.Footer = footer;
            this.FooterTimeStamp = Timestamp;
            this.FooterIconURL = footerIconURL;
            return this;
        }
        public Embed build(){
            return new Embed(Author,AuthorURL,AuthorIconURL,BodyTitle,BodyDescription,BodyURL,BodyColor,Fields,ImageURL,ThumbnailURL,Footer,FooterTimeStamp,FooterIconURL);
        }
    }

    public static class Field{
        protected final String name;
        protected final String value;
        public Field(String name,String value){
            this.name = name;
            this.value = value;
        }
        public JsonObject serialize(){
            JsonObject result = new JsonObject();
            result.addProperty("name",name);
            result.addProperty("value",value);
            return result;
        }
    }
}
