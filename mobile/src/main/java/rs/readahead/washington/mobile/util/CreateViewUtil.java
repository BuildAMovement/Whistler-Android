package rs.readahead.washington.mobile.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import rs.readahead.washington.mobile.R;


public class CreateViewUtil {

    public static View getEvidenceItem(final String path, final Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams") LinearLayout item = (LinearLayout) inflater.inflate(R.layout.evidence_item, null);
        item.setContentDescription(path);

        TextView textView = (TextView) item.findViewById(R.id.evidence_path);
        ImageView imageView = (ImageView) item.findViewById(R.id.evidence_image);

        textView.setText(FileUtil.getEvidenceFileDisplayText(path));
        createImageItem(path, context, imageView);

        return item;
    }

    private static void createImageItem(final String path, final Context context, final ImageView view) {
        if (TextUtils.isEmpty(path)) {
            return;
        }

        String lowerPath = path.toLowerCase(),
                ext = getFileExtension(lowerPath),
                type = getPrimaryMimeType(ext);

        if ("3gp".equals(ext) || "audio".equals(type)) {
            // 3gp is video/3gpp, but we force audio as whistler is recording audio in 3gp
            view.setImageResource(R.drawable.mob_audio);
        } else if ("image".equals(type)) {
            Glide.with(context)
                    .load(path)
                    .placeholder(R.drawable.mob_take_photo)
                    .centerCrop()
                    .thumbnail(0.1f)
                    .into(view);

        } else if ("video".equals(type)) {
            view.setImageResource(R.drawable.mob_video);
        }

        // todo: image placeholder for non media files?
    }

    // todo: move these bellow somewhere "global" if usage spreads

    private static String getFileExtension(final String url) {
        return MimeTypeMap.getFileExtensionFromUrl(url);
    }

    private static String getPrimaryMimeType(final String extension) {
        if (TextUtils.isEmpty(extension)) {
            return null;
        }

        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

        if (TextUtils.isEmpty(type)) {
            return null;
        }

        //noinspection LoopStatementThatDoesntLoop
        for (String token : type.split("/")) {
            return token.toLowerCase();
        }

        return null;
    }

    public static View createTextView(String text, Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams") TextView item = (TextView) inflater.inflate(R.layout.item_text_view, null);
        item.setText(text);
        return item;
    }

}
