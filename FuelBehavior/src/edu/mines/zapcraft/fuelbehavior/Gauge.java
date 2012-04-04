package edu.mines.zapcraft.FuelBehavior;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public final class Gauge extends View {

	private static final String TAG = Gauge.class.getSimpleName();

	// drawing tools
	private RectF rimRect;
	private Paint rimPaint;
	private RectF outerRimRect;
	private RectF innerRimRect;

	private Paint rimCirclePaint;

	private RectF faceRect;
	private Paint facePaint;
	private Paint rimShadowPaint;

	private Paint scalePaint;
	private RectF scaleRect;

	private Paint titlePaint;
	private Path titlePath;

	private Paint handPaint;
	private Path handPath;
	private Paint handLinePaint;
	private Path handLinePath;
	private Paint handScrewPaint;

	private Paint cachedPaint;
	// end drawing tools

	private Bitmap cached; // holds the cached static part

	// configuration
	private int totalNicks = 100;
	private int largeNickInterval = 5;
	private int minDegrees = -140;
	private int maxDegrees = 140;
	private int degreeRange;
	private float degreesPerNick;
	private int minValue = 0;
	private int maxValue = 100;
	private int valueRange;
	private float valuesPerNick;
	private float degreesPerValue;
	private String title = "ZapCraft";

	// hand value
	private float handValue;


	public Gauge(Context context) {
		super(context);
		init(context, null);
	}

	public Gauge(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public Gauge(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}

	public void setHandValue(float value) {
		if (value < minValue) {
			value = minValue;
		} else if (value > maxValue) {
			value = maxValue;
		}
		handValue = value;
		invalidate();
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		Bundle bundle = (Bundle) state;
		Parcelable superState = bundle.getParcelable("superState");
		super.onRestoreInstanceState(superState);

		handValue = bundle.getFloat("handPosition");
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();

		Bundle state = new Bundle();
		state.putParcelable("superState", superState);
		state.putFloat("handPosition", handValue);
		return state;
	}

	private void init(Context context, AttributeSet attrs) {
		// Get the properties from the resource file.
		if (context != null && attrs != null){
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Gauge);

			totalNicks = a.getInt(R.styleable.Gauge_totalNicks, totalNicks);
			largeNickInterval = a.getInt(R.styleable.Gauge_largeNickInterval, largeNickInterval);
			minDegrees = a.getInt(R.styleable.Gauge_minDegrees, minDegrees);
			maxDegrees = a.getInt(R.styleable.Gauge_maxDegrees, maxDegrees);
			minValue = a.getInt(R.styleable.Gauge_minValue, minValue);
			maxValue = a.getInt(R.styleable.Gauge_maxValue, maxValue);
			String title = a.getString(R.styleable.Gauge_title);
			if (title != null) this.title = title;
		}

		degreeRange = maxDegrees - minDegrees;
		degreesPerNick = (float) degreeRange / totalNicks;
		valueRange = maxValue - minValue;
		valuesPerNick = (float) valueRange / totalNicks;
		degreesPerValue = (float) degreeRange / valueRange;

		handValue = minValue;

		initDrawingTools();
	}

	public void setTitle(String title) {
		this.title = title;
	}

	private void initDrawingTools() {
		// the linear gradient is a bit skewed for realism
		rimPaint = new Paint();
		rimPaint.setAntiAlias(true);
		rimPaint.setStyle(Paint.Style.STROKE);

		rimCirclePaint = new Paint();
		rimCirclePaint.setAntiAlias(true);
		rimCirclePaint.setStyle(Paint.Style.STROKE);
		rimCirclePaint.setColor(Color.argb(0x4f, 0x33, 0x36, 0x33));

		facePaint = new Paint();
		facePaint.setAntiAlias(true);
		facePaint.setStyle(Paint.Style.FILL);
		facePaint.setColor(Color.rgb(0xdd, 0x00, 0xbb));

		rimShadowPaint = new Paint();
		rimShadowPaint.setAntiAlias(true);
		rimShadowPaint.setStyle(Paint.Style.FILL);

		scalePaint = new Paint();
		scalePaint.setStyle(Paint.Style.STROKE);
		scalePaint.setColor(Color.BLACK);
		scalePaint.setAntiAlias(true);
		scalePaint.setTypeface(Typeface.SANS_SERIF);
		scalePaint.setTextAlign(Paint.Align.CENTER);

		titlePaint = new Paint();
		titlePaint.setColor(Color.BLACK);
		titlePaint.setAntiAlias(true);
		titlePaint.setTypeface(Typeface.DEFAULT_BOLD);
		titlePaint.setTextAlign(Paint.Align.CENTER);

		handPaint = new Paint();
		handPaint.setAntiAlias(true);
		handPaint.setColor(Color.BLACK);
		handPaint.setStyle(Paint.Style.FILL);

		handLinePaint = new Paint();
		handLinePaint.setAntiAlias(true);
		handLinePaint.setStyle(Paint.Style.FILL);
		handLinePaint.setColor(Color.WHITE);

		handScrewPaint = new Paint();
		handScrewPaint.setAntiAlias(true);
		handScrewPaint.setColor(Color.rgb(0x44, 0x44, 0x44));
		handScrewPaint.setStyle(Paint.Style.FILL);

		cachedPaint = new Paint();
		cachedPaint.setFilterBitmap(true);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		Log.d(TAG, "Width spec: " + MeasureSpec.toString(widthMeasureSpec));
		Log.d(TAG, "Height spec: " + MeasureSpec.toString(heightMeasureSpec));

		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);

		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		int chosenWidth = chooseDimension(widthMode, widthSize);
		int chosenHeight = chooseDimension(heightMode, heightSize);

		int chosenDimension = Math.min(chosenWidth, chosenHeight);

		setMeasuredDimension(chosenDimension, chosenDimension);
	}

	private int chooseDimension(int mode, int size) {
		if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
			return size;
		} else { // (mode == MeasureSpec.UNSPECIFIED)
			return getPreferredSize();
		}
	}

	// in case there is no size specified
	private int getPreferredSize() {
		return 300;
	}

	private void drawRim(Canvas canvas) {
		// first, draw the metallic body
		canvas.drawOval(rimRect, rimPaint);
		// now the outer rim circle
		canvas.drawOval(outerRimRect, rimCirclePaint);
		// draw the inner rim circle
		canvas.drawOval(innerRimRect, rimCirclePaint);
		// draw the rim shadow inside the face
		canvas.drawOval(innerRimRect, rimShadowPaint);
	}

	private void drawScale(Canvas canvas) {
		float w = getWidth();
		float center = 0.5f*w;

		canvas.drawOval(scaleRect, scalePaint);

		canvas.save(Canvas.MATRIX_SAVE_FLAG);

		canvas.rotate(minDegrees, center, center);

		for (int i = 0; i <= totalNicks; ++i) {
			float y1 = scaleRect.top;
			float y2 = y1 - 0.015f*w;

			if (i % largeNickInterval == 0) {
				canvas.drawLine(center, y1, center, y2 - 0.015f*w, scalePaint);

				int value = (int)(i * valuesPerNick + minValue);

				if (value >= minValue && value <= maxValue) {
					String valueString = Integer.toString(value);
					canvas.drawText(valueString, center, y2 - 0.025f*w, scalePaint);
				}
			} else {
				canvas.drawLine(center, y1, center, y2, scalePaint);
			}

			canvas.rotate(degreesPerNick, center, center);
		}
		canvas.restore();
	}

	private void drawTitle(Canvas canvas) {
		canvas.drawTextOnPath(title, titlePath, 0.0f, 0.0f, titlePaint);
	}

	private float valueToAngle(float value) {
		return (value - minValue) * degreesPerValue + minDegrees;
	}

	private void drawHand(Canvas canvas) {
		float w = getWidth();
		float handAngle = valueToAngle(handValue);
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		canvas.rotate(handAngle, 0.5f*w, 0.5f*w);
		canvas.drawPath(handPath, handPaint);
		canvas.drawPath(handLinePath, handLinePaint);
		canvas.restore();
		canvas.drawCircle(0.5f*w, 0.5f*w, 0.01f*w, handScrewPaint);
	}

	private void drawFace(Canvas canvas) {
		int color = Color.rgb((int)(0xff * getRelativePosition()),
			0xff - (int)(0xff * getRelativePosition()), 0x00);

		facePaint.setColor(color);
		canvas.drawOval(faceRect, facePaint);
	}

	private void drawCached(Canvas canvas) {
		if (cached == null) {
			Log.w(TAG, "Background not created");
		} else {
			canvas.drawBitmap(cached, 0, 0, cachedPaint);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		drawFace(canvas);

		drawCached(canvas);

		drawHand(canvas);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		Log.d(TAG, "Size changed to " + w + "x" + h);

		float rimSize = 0.02f*w;

		rimRect = new RectF(0.1f*w, 0.1f*w, 0.9f*w, 0.9f*w);

		outerRimRect = new RectF();
		outerRimRect.set(rimRect.left - rimSize/2, rimRect.top - rimSize/2, rimRect.right + rimSize/2, rimRect.bottom + rimSize/2);

		innerRimRect = new RectF();
		innerRimRect.set(rimRect.left + rimSize/2, rimRect.top + rimSize/2, rimRect.right - rimSize/2, rimRect.bottom - rimSize/2);

		faceRect = new RectF();
		faceRect.set(rimRect.left + rimSize/2, rimRect.top + rimSize/2,
			     rimRect.right - rimSize/2, rimRect.bottom - rimSize/2);

		float scalePosition = 0.10f*w;
		scaleRect = new RectF();
		scaleRect.set(faceRect.left + scalePosition, faceRect.top + scalePosition,
					  faceRect.right - scalePosition, faceRect.bottom - scalePosition);

		titlePath = new Path();
		titlePath.addArc(new RectF(0.24f*w, 0.24f*w, 0.76f*w, 0.76f*w), -180.0f, -180.0f);

		handPath = new Path();
		handPath.moveTo(0.5f*w, (0.5f + 0.2f)*w);
		handPath.lineTo((0.5f - 0.010f)*w, (0.5f + 0.2f - 0.007f)*w);
		handPath.lineTo((0.5f - 0.008f)*w, (0.5f - 0.32f)*w);
		handPath.lineTo((0.5f + 0.008f)*w, (0.5f - 0.32f)*w);
		handPath.lineTo((0.5f + 0.010f)*w, (0.5f + 0.2f - 0.007f)*w);
		handPath.lineTo(0.5f*w, (0.5f + 0.2f)*w);
		handPath.addCircle(0.5f*w, 0.5f*w, 0.025f*w, Path.Direction.CW);

		handLinePath = new Path();
		handLinePath.moveTo((0.5f + 0.004f)*w, (0.5f - 0.1f)*w);
		handLinePath.lineTo((0.5f - 0.004f)*w, (0.5f - 0.1f)*w);
		handLinePath.lineTo((0.5f - 0.003f)*w, (0.5f - 0.315f)*w);
		handLinePath.lineTo((0.5f + 0.003f)*w, (0.5f - 0.315f)*w);
		handLinePath.lineTo((0.5f + 0.004f)*w, (0.5f - 0.1f)*w);

		rimPaint.setShader(new LinearGradient(0.40f*w, 0.0f*w, 0.60f*w, 1.0f*w,
										   Color.rgb(0xf0, 0xf5, 0xf0),
										   Color.rgb(0x30, 0x31, 0x30),
										   Shader.TileMode.CLAMP));
		rimPaint.setStrokeWidth(rimSize);

		rimCirclePaint.setStrokeWidth(0.005f*w);

		rimShadowPaint.setShader(new RadialGradient(0.5f*w, 0.5f*w, faceRect.width() / 2.0f,
				   new int[] { 0x00000000, 0x00000500, 0x50000500 },
				   new float[] { 0.96f*w, 0.96f*w, 0.99f*w },
				   Shader.TileMode.MIRROR));

		scalePaint.setStrokeWidth(0.005f*w);
		scalePaint.setTextSize(0.045f*w);

		handPaint.setShadowLayer(0.01f*w, -0.005f*w, -0.005f*w, 0x7f000000);

		titlePaint.setTextSize(0.05f*w);

		regenerateCache();
	}

	private void regenerateCache() {
		// free the old bitmap
		if (cached != null) {
			cached.recycle();
		}

		cached = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
		Canvas cachedCanvas = new Canvas(cached);

		drawRim(cachedCanvas);
		drawScale(cachedCanvas);
		drawTitle(cachedCanvas);
	}

	/**
	 * Returns the position of the hand as a float between 0.0 and 1.0.
	 *
	 * @return the position of the hand as a float between 0.0 and 1.0.
	 */
	private float getRelativePosition() {
		return (float) (handValue - minValue) / valueRange;
	}
}
