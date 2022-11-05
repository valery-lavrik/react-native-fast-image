package com.dylanvann.fastimage;

import android.app.Activity;
import java.io.File;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.views.imagehelper.ImageSource;

import com.bumptech.glide.request.RequestListener;
import android.graphics.drawable.Drawable;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;


class FastImageViewModule extends ReactContextBaseJavaModule {

    private static final String REACT_CLASS = "FastImageView";
    private static final String ERROR_LOAD_FAILED = "ERROR_LOAD_FAILED";

    FastImageViewModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @ReactMethod
    public void preload(final ReadableArray sources) {
        final Activity activity = getCurrentActivity();
        if (activity == null) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < sources.size(); i++) {
                    final ReadableMap source = sources.getMap(i);
                    final FastImageSource imageSource = FastImageViewConverter.getImageSource(activity, source);

                    Glide
                            .with(activity.getApplicationContext())
                            // This will make this work for remote and local images. e.g.
                            //    - file:///
                            //    - content://
                            //    - res:/
                            //    - android.resource://
                            //    - data:image/png;base64
                            .load(
                                    imageSource.isBase64Resource() ? imageSource.getSource() :
                                    imageSource.isResource() ? imageSource.getUri() : imageSource.getGlideUrl()
                            )
                            .apply(FastImageViewConverter.getOptions(activity, imageSource, source))
                            .preload();
                }
            }
        });
    }


	@ReactMethod
    public void preloadDimension(final ReadableMap source, final Promise promise) {
        final Activity activity = getCurrentActivity();
        if (activity == null) return;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final FastImageSource imageSource = FastImageViewConverter.getImageSource(activity, source);
                final GlideUrl glideUrl = imageSource.getGlideUrl();

                if (glideUrl == null) {
                    promise.resolve(null);
                    return;
                }

                Glide
					.with(activity.getApplicationContext())
					.load(
						imageSource.isBase64Resource() ? imageSource.getSource() :
						imageSource.isLocalFile() ? new File(source.getString("uri")) : // https://stackoverflow.com/questions/32332003/glide-load-local-image-by-uri
						imageSource.isResource() ? imageSource.getUri() : 
						imageSource.getGlideUrl()
					)
					.apply(FastImageViewConverter.getOptions(activity, imageSource,  source))
					.listener(new RequestListener<Drawable>() {
						@Override
						public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
							promise.reject(ERROR_LOAD_FAILED, e);
							return false;
						}

						@Override
						public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {

							WritableMap params = Arguments.createMap();
							params.putInt("width", resource.getIntrinsicWidth());
							params.putInt("height", resource.getIntrinsicHeight()); 
							promise.resolve(params);

							return false;
						}
					})
					.preload();

            }
        });
  	}



    @ReactMethod
    public void clearMemoryCache(final Promise promise) {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            promise.resolve(null);
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Glide.get(activity.getApplicationContext()).clearMemory();
                promise.resolve(null);
            }
        });
    }

    @ReactMethod
    public void clearDiskCache(Promise promise) {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            promise.resolve(null);
            return;
        }

        Glide.get(activity.getApplicationContext()).clearDiskCache();
        promise.resolve(null);
    }
}
