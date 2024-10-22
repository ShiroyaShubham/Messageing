package messenger.messages.messaging.sms.chat.meet.send_message;

import android.graphics.Bitmap;
import android.net.Uri;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Message {

    public static final class Part {
        private byte[] media;
        private String contentType;
        private String name;
        public Part(byte[] media, String contentType, String name) {
            this.media = media;
            this.contentType = contentType;
            this.name = name;
        }

        public byte[] getMedia() {
            return media;
        }

        public String getContentType() {
            return contentType;
        }

        public String getName() {
            return name;
        }
    }
    private String text;
    private String subject;
    private String fromAddress;
    private String[] addresses;
    private Bitmap[] images;
    private String[] imageNames;
    private List<Part> parts = new ArrayList<Part>();
    private boolean save;
    private int delay;
    private Uri messageUri;

    public Message() {
        this("", new String[]{""});
    }

    public Message(String text, String address) {
        this(text, address.trim().split(" "));
    }

    public Message(String text, String address, String subject) {
        this(text, address.trim().split(" "), subject);
    }

    public Message(String text, String[] addresses) {
        this.text = text;
        this.addresses = addresses;
        this.images = new Bitmap[0];
        this.subject = null;
        this.save = true;
        this.delay = 0;
    }

    public Message(String text, String[] addresses, String subject) {
        this.text = text;
        this.addresses = addresses;
        this.images = new Bitmap[0];
        this.subject = subject;
        this.save = true;
        this.delay = 0;
    }

    public Message(String text, String address, Bitmap image) {
        this(text, address.trim().split(" "), new Bitmap[]{image});
    }

    public Message(String text, String address, Bitmap image, String subject) {
        this(text, address.trim().split(" "), new Bitmap[]{image}, subject);
    }

    public Message(String text, String[] addresses, Bitmap image) {
        this(text, addresses, new Bitmap[]{image});
    }

    public Message(String text, String[] addresses, Bitmap image, String subject) {
        this(text, addresses, new Bitmap[]{image}, subject);
    }

    public Message(String text, String address, Bitmap[] images) {
        this(text, address.trim().split(" "), images);
    }

    public Message(String text, String address, Bitmap[] images, String subject) {
        this(text, address.trim().split(" "), images, subject);
    }

    public Message(String text, String[] addresses, Bitmap[] images) {
        this.text = text;
        this.addresses = addresses;
        this.images = images;
        this.subject = null;
        this.save = true;
        this.delay = 0;
    }

    public Message(String text, String[] addresses, Bitmap[] images, String subject) {
        this.text = text;
        this.addresses = addresses;
        this.images = images;
        this.subject = subject;
        this.save = true;
        this.delay = 0;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setAddresses(String[] addresses) {
        this.addresses = addresses;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public void setAddress(String address) {
        this.addresses = new String[1];
        this.addresses[0] = address;
    }

    public void setImages(Bitmap[] images) {
        this.images = images;
    }

    public void setImageNames(String[] names) {
        this.imageNames = names;
    }

    public void setImage(Bitmap image) {
        this.images = new Bitmap[1];
        this.images[0] = image;
    }

    @Deprecated
    public void setAudio(byte[] audio) {
        addAudio(audio);
    }

    public void addAudio(byte[] audio) {
        addAudio(audio, null);
    }

    public void addAudio(byte[] audio, String name) {
        addMedia(audio, "audio/wav", name);
    }

    @Deprecated
    public void setVideo(byte[] video) {
        addVideo(video);
    }

    public void addVideo(byte[] video) {
        addVideo(video, null);
    }

    public void addVideo(byte[] video, String name) {
        addMedia(video, "video/3gpp", name);
    }

    @Deprecated
    public void setMedia(byte[] media, String mimeType) {
         addMedia(media, mimeType);
    }

    public void addMedia(byte[] media, String mimeType) {
        this.parts.add(new Part(media, mimeType, null));
    }

    public void addMedia(byte[] media, String mimeType, String name) {
        this.parts.add(new Part(media, mimeType, name));
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setSave(boolean save) {
        this.save = save;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public void addAddress(String address) {
        String[] temp = this.addresses;

        if (temp == null) {
            temp = new String[0];
        }

        this.addresses = new String[temp.length + 1];

        for (int i = 0; i < temp.length; i++) {
            this.addresses[i] = temp[i];
        }

        this.addresses[temp.length] = address;
    }

    public void addImage(Bitmap image) {
        Bitmap[] temp = this.images;

        if (temp == null) {
            temp = new Bitmap[0];
        }

        this.images = new Bitmap[temp.length + 1];

        for (int i = 0; i < temp.length; i++) {
            this.images[i] = temp[i];
        }

        this.images[temp.length] = image;
    }

    public String getText() {
        return this.text;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public String[] getAddresses() {
        return this.addresses;
    }

    public Bitmap[] getImages() {
        return this.images;
    }

    public String[] getImageNames() {
        return this.imageNames;
    }

    public List<Part> getParts() {
        return this.parts;
    }

    public String getSubject() {
        return this.subject;
    }

    public boolean getSave() {
        return this.save;
    }

    public int getDelay() {
        return this.delay;
    }

    public static byte[] bitmapToByteArray(Bitmap image) {
		byte[] output = new byte[0];
        if (image == null) {
            Log.v("Message", "image is null, returning byte array of size 0");
            return output;
        }
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		try {
			image.compress(Bitmap.CompressFormat.JPEG, 90, stream);
			output = stream.toByteArray();
		} finally {
			try {
				stream.close();
			} catch (IOException e) {}
		}
		return output;
    }

    public Uri getMessageUri() {
        return messageUri;
    }

    public void setMessageUri(Uri messageUri) {
        this.messageUri = messageUri;
    }
}
