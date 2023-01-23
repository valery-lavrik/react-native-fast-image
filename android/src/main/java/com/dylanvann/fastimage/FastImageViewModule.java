package com.dylanvann.fastimage;

import android.app.Activity;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;

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
import android.graphics.Bitmap;
import android.graphics.Canvas;
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
						imageSource.isLocalFile() ? source.getString("uri") :
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


							String saveToFile = source.getString("saveToFile");
							if (saveToFile.isEmpty()) {
								promise.resolve(params);
								return false;
							}
							


							// https://stackoverflow.com/questions/10174399/how-can-i-write-a-drawable-resource-to-a-file
							String fileName = saveToFile.substring(saveToFile.lastIndexOf("/") + 1, saveToFile.length());
							String pathFolder = saveToFile.substring(0, saveToFile.length() - fileName.length());


							// 1 - создам нужную папку
							File dir = new File(pathFolder);
							if (!dir.exists()) {
								boolean doSave = dir.mkdirs();

								if (!doSave) {
									promise.resolve(params);
									return false;
								}
							}


							// 2 - создам Bitmap объект
							// https://stackoverflow.com/questions/3035692/how-to-convert-a-drawable-to-a-bitmap
							Bitmap bm = Bitmap.createBitmap(resource.getIntrinsicWidth(), resource.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
							Canvas canvas = new Canvas(bm);
							resource.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
							resource.draw(canvas);


							// 3 - записываем в Bitmap файл
							File imageFile = new File(dir, fileName);
							FileOutputStream fos = null;
							try {
								fos = new FileOutputStream(imageFile);
								bm.compress(Bitmap.CompressFormat.JPEG, 100, fos);
								fos.close();
								
								params.putString("path", pathFolder + fileName);
								promise.resolve(params);
								return false;
							}
							catch (IOException e) {
								if (fos != null) {
									try {
										fos.close();
									} catch (IOException e1) {}
								}
							}
							
						
							// если не получилось просто не возвращаем параметр path
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
