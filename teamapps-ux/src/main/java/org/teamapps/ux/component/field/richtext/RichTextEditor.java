/*-
 * ========================LICENSE_START=================================
 * TeamApps
 * ---
 * Copyright (C) 2014 - 2025 TeamApps.org
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package org.teamapps.ux.component.field.richtext;

import com.ibm.icu.util.ULocale;
import org.teamapps.dto.UiEvent;
import org.teamapps.dto.UiField;
import org.teamapps.dto.UiRichTextEditor;
import org.teamapps.dto.UiTextInputHandlingField;
import org.teamapps.event.Event;
import org.teamapps.ux.component.field.AbstractField;
import org.teamapps.ux.component.field.SpecialKey;
import org.teamapps.ux.component.field.upload.UploadedFile;
import org.teamapps.ux.component.field.upload.UploadedFileAccessException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

public class RichTextEditor extends AbstractField<String> {

	public final Event<String> onTextInput = new Event<>();
	public final Event<SpecialKey> onSpecialKeyPressed = new Event<>();
	public final Event<ImageUploadTooLargeEventData> onImageUploadTooLarge = new Event<>();
	public final Event<ImageUploadStartedEventData> onImageUploadStarted = new Event<>();
	public final Event<ImageUploadSuccessfulEventData> onImageUploadSuccessful = new Event<>();
	public final Event<ImageUploadFailedEventData> onImageUploadFailed = new Event<>();

	private ToolbarVisibilityMode toolbarVisibilityMode = ToolbarVisibilityMode.VISIBLE;
	private int minHeight = 150;
	private int maxHeight;
	private String uploadUrl = "/upload";
	private int maxImageFileSizeInBytes = 5000000;
	private boolean imageUploadEnabled = false;
	private boolean printPluginEnabled = false;
	private ULocale locale = getSessionContext().getULocale();

	private UploadedFileToUrlConverter uploadedFileToUrlConverter = (file) -> getSessionContext().createFileLink(getSessionContext().getUploadedFileByUuid(file.getUuid()));

	public RichTextEditor() {
		super();
	}

	@Override
	public UiField createUiComponent() {
		UiRichTextEditor field = new UiRichTextEditor();
		mapAbstractFieldAttributesToUiField(field);
		field.setToolbarVisibilityMode(this.toolbarVisibilityMode.toToolbarVisibilityMode());
		field.setImageUploadEnabled(imageUploadEnabled);
		field.setUploadUrl(uploadUrl);
		field.setMaxImageFileSizeInBytes(maxImageFileSizeInBytes);
		field.setMinHeight(minHeight);
		field.setMaxHeight(maxHeight);
		field.setLocale(locale.toLanguageTag());
		field.setPrintPluginEnabled(printPluginEnabled);
		return field;
	}

	@Override
	public void handleUiEvent(UiEvent event) {
		super.handleUiEvent(event);
			switch (event.getUiEventType()) {
				case UI_RICH_TEXT_EDITOR_IMAGE_UPLOAD_TOO_LARGE:
					UiRichTextEditor.ImageUploadTooLargeEvent tooLargeEvent = (UiRichTextEditor.ImageUploadTooLargeEvent) event;
					onImageUploadTooLarge.fire(new ImageUploadTooLargeEventData(tooLargeEvent.getFileName(), tooLargeEvent.getMimeType(), tooLargeEvent.getSizeInBytes()));
					break;
				case UI_RICH_TEXT_EDITOR_IMAGE_UPLOAD_STARTED:
					UiRichTextEditor.ImageUploadStartedEvent uploadStartedEvent = (UiRichTextEditor.ImageUploadStartedEvent) event;
					onImageUploadStarted.fire(new ImageUploadStartedEventData(uploadStartedEvent.getFileName(), uploadStartedEvent.getMimeType(), uploadStartedEvent.getSizeInBytes(),
							uploadStartedEvent.getIncompleteUploadsCount()));
					break;
				case UI_RICH_TEXT_EDITOR_IMAGE_UPLOAD_SUCCESSFUL:
					UiRichTextEditor.ImageUploadSuccessfulEvent imageUploadedEvent = (UiRichTextEditor.ImageUploadSuccessfulEvent) event;
					onImageUploadSuccessful.fire(new ImageUploadSuccessfulEventData(imageUploadedEvent.getFileUuid(), imageUploadedEvent.getName(), imageUploadedEvent.getMimeType(),
							imageUploadedEvent.getSizeInBytes(), imageUploadedEvent.getIncompleteUploadsCount()));
					String fileUuid = imageUploadedEvent.getFileUuid();
					UploadedFile uploadedFile = new UploadedFile(imageUploadedEvent.getFileUuid(), imageUploadedEvent.getName(), imageUploadedEvent.getSizeInBytes(), imageUploadedEvent.getMimeType(),
							() -> {
								try {
									return new FileInputStream(getSessionContext().getUploadedFileByUuid(imageUploadedEvent.getFileUuid()));
								} catch (FileNotFoundException e) {
									throw new UploadedFileAccessException(e);
								}
							},
							() -> getSessionContext().getUploadedFileByUuid(imageUploadedEvent.getFileUuid())
					);
					queueCommandIfRendered(() -> new UiRichTextEditor.SetUploadedImageUrlCommand(getId(), fileUuid, this.uploadedFileToUrlConverter.convert(uploadedFile)));
					break;
				case UI_RICH_TEXT_EDITOR_IMAGE_UPLOAD_FAILED:
					UiRichTextEditor.ImageUploadFailedEvent uploadFailedEvent = (UiRichTextEditor.ImageUploadFailedEvent) event;
					onImageUploadFailed.fire(new ImageUploadFailedEventData(uploadFailedEvent.getName(), uploadFailedEvent.getMimeType(), uploadFailedEvent.getSizeInBytes(), uploadFailedEvent
							.getIncompleteUploadsCount()));
					break;
				case UI_TEXT_INPUT_HANDLING_FIELD_TEXT_INPUT:
					if (!this.isValueLocked()) { // Check if this is a stale event since the server already set a new value, but this event is from before applying it on the client side!!
						UiTextInputHandlingField.TextInputEvent keyStrokeEvent = (UiTextInputHandlingField.TextInputEvent) event;
						onTextInput.fire(keyStrokeEvent.getEnteredString());
						break;
					}
				case UI_TEXT_INPUT_HANDLING_FIELD_SPECIAL_KEY_PRESSED:
					UiTextInputHandlingField.SpecialKeyPressedEvent specialKeyPressedEvent = (UiTextInputHandlingField.SpecialKeyPressedEvent) event;
					onSpecialKeyPressed.fire(SpecialKey.valueOf(specialKeyPressedEvent.getKey().name()));
					break;
			}
	}

	public ToolbarVisibilityMode getToolbarVisibilityMode() {
		return toolbarVisibilityMode;
	}

	public void setToolbarVisibilityMode(ToolbarVisibilityMode toolbarVisibilityMode) {
		this.toolbarVisibilityMode = toolbarVisibilityMode;
		queueCommandIfRendered(() -> new UiRichTextEditor.SetToolbarVisibilityModeCommand(getId(), toolbarVisibilityMode.toToolbarVisibilityMode()));
	}

	public UploadedFileToUrlConverter getUploadedFileToUrlConverter() {
		return uploadedFileToUrlConverter;
	}

	public void setUploadedFileToUrlConverter(UploadedFileToUrlConverter uploadedFileToUrlConverter) {
		this.uploadedFileToUrlConverter = uploadedFileToUrlConverter;
	}

	public String getUploadUrl() {
		return uploadUrl;
	}

	public void setUploadUrl(String uploadUrl) {
		this.uploadUrl = uploadUrl;
		queueCommandIfRendered(() -> new UiRichTextEditor.SetUploadUrlCommand(getId(), uploadUrl));
	}

	public int getMaxImageFileSizeInBytes() {
		return maxImageFileSizeInBytes;
	}

	public void setMaxImageFileSizeInBytes(int maxImageFileSizeInBytes) {
		this.maxImageFileSizeInBytes = maxImageFileSizeInBytes;
		queueCommandIfRendered(() -> new UiRichTextEditor.SetMaxImageFileSizeInBytesCommand(getId(), maxImageFileSizeInBytes));
	}

	public int getMinHeight() {
		return minHeight;
	}

	public void setMinHeight(int minHeight) {
		this.minHeight = minHeight;
		queueCommandIfRendered(() -> new UiRichTextEditor.SetMinHeightCommand(getId(), minHeight));
	}

	public int getMaxHeight() {
		return maxHeight;
	}

	public void setMaxHeight(int maxHeight) {
		this.maxHeight = maxHeight;
		queueCommandIfRendered(() -> new UiRichTextEditor.SetMaxHeightCommand(getId(), maxHeight));
	}

	public void setFixedHeight(int height) {
		this.minHeight = height;
		this.maxHeight = height;
		queueCommandIfRendered(() -> new UiRichTextEditor.SetMinHeightCommand(getId(), height));
		queueCommandIfRendered(() -> new UiRichTextEditor.SetMaxHeightCommand(getId(), height));
	}

	public Locale getLocale() {
		return locale.toLocale();
	}

	public ULocale getULocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		setULocale(ULocale.forLocale(locale));
	}

	public void setULocale(ULocale locale) {
		boolean changed = !Objects.equals(locale, this.locale);
		this.locale = locale;
		if (changed) {
			reRenderIfRendered();
		}
	}

	public boolean isImageUploadEnabled() {
		return imageUploadEnabled;
	}

	public void setImageUploadEnabled(boolean imageUploadEnabled) {
		boolean changed = imageUploadEnabled != this.imageUploadEnabled;
		this.imageUploadEnabled = imageUploadEnabled;
		if (changed) {
			reRenderIfRendered();
		}
	}

	/**
	 * Commits and returns (via callback) the transient value from the client side.
	 */
	public void commitTransientValue(Consumer<String> callback) {
		if (isRendered()) {
			getSessionContext().queueCommand(new UiRichTextEditor.CommitTransientValueCommand(getId()), value -> callback.accept(convertUiValueToUxValue(value)));
		} else {
			callback.accept(getValue());
		}
	}

	public void setPrintPluginEnabled(boolean printPluginEnabled) {
		this.printPluginEnabled = printPluginEnabled;
	}
}
