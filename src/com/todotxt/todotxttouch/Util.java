/**
 *
 * Todo.txt Touch/src/com/todotxt/todotxttouch/Util.java
 *
 * Copyright (c) 2009-2011 mathias, Gina Trapani, Stephen Henderson, Tormod Haugen
 *
 * LICENSE:
 *
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).
 *
 * Todo.txt Touch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 *
 * Todo.txt Touch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with Todo.txt Touch.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * @author mathias <mathias[at]ws7862[dot](none)>
 * @author mathias <mathias[at]x2[dot](none)>
 * @author Gina Trapani <ginatrapani[at]gmail[dot]com>
 * @author Stephen Henderson <me[at]steveh[dot]ca>
 * @author Tormod Haugen <tormodh[at]gmail[dot]com>
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2011 mathias, Gina Trapani, Stephen Henderson, Tormod Haugen
 */
package com.todotxt.todotxttouch;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Environment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class Util {

	private static String TAG = Util.class.getSimpleName();

	private static final int CONNECTION_TIMEOUT = 120000;

	private static final int SOCKET_TIMEOUT = 120000;

	private Util() {
	}

	public static boolean isEmpty(String in) {
		return in == null || in.length() == 0;
	}

	public static HttpParams getTimeoutHttpParams() {
		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT);
		return params;
	}

	public static void closeStream(Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
				stream = null;
			} catch (IOException e) {
				Log.w(TAG, "Close stream exception", e);
			}
		}
	}

	public static InputStream getInputStreamFromUrl(String url)
			throws ClientProtocolException, IOException {
		HttpGet request = new HttpGet(url);
		DefaultHttpClient client = new DefaultHttpClient(getTimeoutHttpParams());
		HttpResponse response = client.execute(request);
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != 200) {
			Log.e(TAG, "Failed to get stream for: " + url);
			throw new IOException("Failed to get stream for: " + url);
		}
		return response.getEntity().getContent();
	}

	public static String fetchContent(String url)
			throws ClientProtocolException, IOException {
		InputStream input = getInputStreamFromUrl(url);
		try {
			int c;
			byte[] buffer = new byte[8192];
			StringBuilder sb = new StringBuilder();
			while ((c = input.read(buffer)) != -1) {
				sb.append(new String(buffer, 0, c));
			}
			return sb.toString();
		} finally {
			closeStream(input);
		}
	}

	public static String readStream(InputStream is) {
		if (is == null) {
			return null;
		}
		try {
			int c;
			byte[] buffer = new byte[8192];
			StringBuilder sb = new StringBuilder();
			while ((c = is.read(buffer)) != -1) {
				sb.append(new String(buffer, 0, c));
			}
			return sb.toString();
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			closeStream(is);
		}
		return null;
	}

	public static void writeFile(InputStream is, File file)
			throws ClientProtocolException, IOException {
		FileOutputStream os = new FileOutputStream(file);
		try {
			int c;
			byte[] buffer = new byte[8192];
			while ((c = is.read(buffer)) != -1) {
				os.write(buffer, 0, c);
			}
		} finally {
			closeStream(is);
			closeStream(os);
		}
	}

	public static void showToastLong(Context cxt, int resid) {
		Toast.makeText(cxt, resid, Toast.LENGTH_LONG).show();
	}

	public static void showToastShort(Context cxt, int resid) {
		Toast.makeText(cxt, resid, Toast.LENGTH_SHORT).show();
	}

	public static void showToastLong(Context cxt, String msg) {
		Toast.makeText(cxt, msg, Toast.LENGTH_LONG).show();
	}

	public static void showToastShort(Context cxt, String msg) {
		Toast.makeText(cxt, msg, Toast.LENGTH_SHORT).show();
	}

	public interface OnMultiChoiceDialogListener {
		void onClick(boolean[] selected);
	}

	public static Dialog createMultiChoiceDialog(Context cxt,
			CharSequence[] keys, boolean[] values, Integer titleId,
			Integer iconId, final OnMultiChoiceDialogListener listener) {
		final boolean[] res;
		if (values == null) {
			res = new boolean[keys.length];
		} else {
			res = values;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(cxt);
		if (iconId != null) {
			builder.setIcon(iconId);
		}
		if (titleId != null) {
			builder.setTitle(titleId);
		}
		builder.setMultiChoiceItems(keys, values,
				new DialogInterface.OnMultiChoiceClickListener() {
					public void onClick(DialogInterface dialog,
							int whichButton, boolean isChecked) {
						res[whichButton] = isChecked;
					}
				});
		builder.setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						listener.onClick(res);
					}
				});
		builder.setNegativeButton(R.string.cancel, null);
		return builder.create();
	}

	public static void showDialog(Context cxt, int titleid, int msgid) {
		AlertDialog.Builder builder = new AlertDialog.Builder(cxt);
		builder.setTitle(titleid);
		builder.setMessage(msgid);
		builder.setPositiveButton(android.R.string.ok, null);
		builder.setCancelable(true);
		builder.show();
	}

	public static void showDialog(Context cxt, int titleid, String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(cxt);
		builder.setTitle(titleid);
		builder.setMessage(msg);
		builder.setPositiveButton(android.R.string.ok, null);
		builder.setCancelable(true);
		builder.show();
	}

	public static void showConfirmationDialog(Context cxt, int msgid,
			OnClickListener oklistener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(cxt);
		// builder.setTitle(cxt.getPackageName());
		builder.setMessage(msgid);
		builder.setPositiveButton(android.R.string.ok, oklistener);
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.setCancelable(true);
		builder.show();
	}

	public static void showDeleteConfirmationDialog(Context cxt,
			OnClickListener oklistener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(cxt);
		builder.setTitle(R.string.delete_task_title);
		builder.setMessage(R.string.delete_task_message);
		builder.setPositiveButton(R.string.delete_task_confirm, oklistener);
		builder.setNegativeButton(R.string.cancel, null);
		builder.show();
	}

	public static boolean isDeviceWritable() {
		String sdState = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(sdState)) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isDeviceReadable() {
		String sdState = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(sdState)
				|| Environment.MEDIA_MOUNTED_READ_ONLY.equals(sdState)) {
			return true;
		} else {
			return false;
		}
	}

	public interface InputDialogListener {
		void onClick(String input);
	}

	public static void showInputDialog(Context cxt, int titleid, int msgid,
			String defaulttext, int lines,
			final InputDialogListener oklistener, int icon) {
		LayoutInflater factory = LayoutInflater.from(cxt);
		final View textEntryView = factory.inflate(R.layout.inputdialog, null);
		final TextView textinput = (TextView) textEntryView
				.findViewById(R.id.input);
		textinput.setText(defaulttext);
		textinput.setLines(lines);
		AlertDialog.Builder builder = new AlertDialog.Builder(cxt);
		if (icon > 0) {
			builder.setIcon(icon);
		}
		builder.setTitle(titleid);
		builder.setMessage(msgid);
		builder.setView(textEntryView);
		builder.setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String input = textinput.getText().toString();
						oklistener.onClick(input);
					}
				});
		builder.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				});
		builder.setCancelable(true);
		builder.show();
	}

	public interface LoginDialogListener {
		void onClick(String username, String password);
	}

	public static void createParentDirectory(File dest) throws TodoException {
		if (dest == null) {
			throw new TodoException("createParentDirectory: dest is null");
		}
		File dir = dest.getParentFile();
		if (dir != null && !dir.exists()) {
			createParentDirectory(dir);
		}
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				Log.e(TAG, "Could not create dirs: " + dir.getAbsolutePath());
				throw new TodoException("Could not create dirs: "
						+ dir.getAbsolutePath());
			}
		}
	}

	public static ArrayAdapter<String> newSpinnerAdapter(Context cxt,
			List<String> items) {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(cxt,
				android.R.layout.simple_spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		return adapter;
	}

	public static void setBold(SpannableString ss, List<String> items) {
		String data = ss.toString();
		for (String item : items) {
			int i = data.indexOf("@" + item);
			if (i != -1) {
				// ss.setSpan(what, start, end, flags);
				ss.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
						i + 1, i + 1 + item.length(),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			int j = data.indexOf("+" + item);
			if (j != -1) {
				ss.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
						j + 1, j + 1 + item.length(),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
	}

}
