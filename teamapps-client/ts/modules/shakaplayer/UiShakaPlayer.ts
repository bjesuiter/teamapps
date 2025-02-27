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

import "shaka-player/dist/controls.css"
import "@less/components/UiShakaPlayer.less"

import {executeWhenFirstDisplayed} from "../util/ExecuteWhenFirstDisplayed";
import {AbstractUiComponent} from "../AbstractUiComponent";
import {TeamAppsEvent} from "../util/TeamAppsEvent";
import {TeamAppsUiContext} from "../TeamAppsUiContext";
import {
	UiShakaPlayer_EndedEvent,
	UiShakaPlayer_ErrorLoadingEvent, UiShakaPlayer_ManifestLoadedEvent, UiShakaPlayer_SkipClickedEvent,
	UiShakaPlayer_TimeUpdateEvent,
	UiShakaPlayerCommandHandler,
	UiShakaPlayerConfig,
	UiShakaPlayerEventSource,
} from "../../generated/UiShakaPlayerConfig";
import {TeamAppsUiComponentRegistry} from "../TeamAppsUiComponentRegistry";
import {addDelegatedEventListener, parseHtml} from "../Common";
import {UiPosterImageSize} from "../../generated/UiPosterImageSize";
import {throttle} from "../util/throttle";
import {UiTrackLabelFormat} from "../../generated/UiTrackLabelFormat";

(window as any).shaka = require("shaka-player");
(window as any).shaka = require("shaka-player/dist/shaka-player.ui");

import "./SkipBackButton";
import "./SkipForwardButton";

import Player = shaka.Player;
import UIConfiguration = shaka.extern.UIConfiguration;
import Overlay = shaka.ui.Overlay;
import ManifestConfiguration = shaka.extern.ManifestConfiguration;
import ManifestParser = shaka.extern.ManifestParser;
import PlayerInterface = shaka.extern.ManifestParser.PlayerInterface;
import TrackLabelFormat = shaka.ui.Overlay.TrackLabelFormat;
import {UiShakaManifestConfig} from "../../generated/UiShakaManifestConfig";
import Manifest = shaka.extern.Manifest;

export class UiShakaPlayer extends AbstractUiComponent<UiShakaPlayerConfig> implements UiShakaPlayerCommandHandler, UiShakaPlayerEventSource {

	public readonly onManifestLoaded: TeamAppsEvent<UiShakaPlayer_ManifestLoadedEvent> = new TeamAppsEvent();
	public readonly onTimeUpdate: TeamAppsEvent<UiShakaPlayer_TimeUpdateEvent> = new TeamAppsEvent();
	public readonly onEnded: TeamAppsEvent<UiShakaPlayer_EndedEvent> = new TeamAppsEvent();
	public readonly onErrorLoading: TeamAppsEvent<UiShakaPlayer_ErrorLoadingEvent> = new TeamAppsEvent();
	public readonly onSkipClicked: TeamAppsEvent<UiShakaPlayer_SkipClickedEvent> = new TeamAppsEvent();

	private $componentWrapper: HTMLElement;
	private $video: HTMLMediaElement;
	private player: Player;

	private ui: Overlay;

	constructor(config: UiShakaPlayerConfig, context: TeamAppsUiContext) {
		super(config, context);

		console.log(config.hlsUrl, config.dashUrl, config.posterImageUrl)

		const posterImageSizeCssClass = `poster-${UiPosterImageSize[config.posterImageSize].toLowerCase()}`;
		this.$componentWrapper = parseHtml(
			`<div class="UiShakaPlayer" style="background-color: ${config.backgroundColor}">
				<div class="video-wrapper">
					<video id="video" poster="${config.posterImageUrl}" class="${posterImageSizeCssClass}"></video>
				</div>
			</div>`);
		this.$video = this.$componentWrapper.querySelector(":scope video");

		this.$video.addEventListener("timeupdate", throttle(e => this.onTimeUpdate.fire({timeMillis: this.$video.currentTime * 1000}), config.timeUpdateEventThrottleMillis))
		this.$video.addEventListener("ended", e => this.onEnded.fire({}))

		this.displayedDeferredExecutor.invokeWhenReady(() => {
			this.player = new shaka.Player(this.$video);
			this.reconfigurePlayer();
			this.player.addEventListener('error', () => this.onErrorLoading.fire({}));
			this.player.addEventListener('streaming', () => {
				this.reconfigureUi();
				this.jumpTo(this._config.timeMillis);
				this.onManifestLoaded.fire({manifest: this.createUiManifest(this.player.getManifest())});
			});

			this.ui = new shaka.ui.Overlay(this.player, this.$componentWrapper, this.$video);
			const uiConfig: Partial<UIConfiguration> = {
				addBigPlayButton: config.bigPlayButtonEnabled,
				controlPanelElements: config.controlPanelElements,
				doubleClickForFullscreen: true,
				enableFullscreenOnRotation: false,
				enableKeyboardPlaybackControls: true,
				fadeDelay: config.controlFadeDelaySeconds,
				forceLandscapeOnFullscreen: true,
				overflowMenuButtons: this.getOverflowMenuButtonsConfig(),
				seekBarColors: undefined,
				showUnbufferedStart: false,
				trackLabelFormat: config.trackLabelFormat == UiTrackLabelFormat.LABEL ? TrackLabelFormat.LABEL
					: config.trackLabelFormat == UiTrackLabelFormat.LANGUAGE ? TrackLabelFormat.LANGUAGE
						: config.trackLabelFormat == UiTrackLabelFormat.ROLE ? TrackLabelFormat.ROLE
							: config.trackLabelFormat == UiTrackLabelFormat.LANGUAGE_ROLE ? TrackLabelFormat.LANGUAGE_ROLE
								: undefined,
				volumeBarColors: undefined,
				addSeekBar: true
			};
			this.ui.configure(uiConfig as UIConfiguration);

			addDelegatedEventListener(this.ui.getControls().getControlsContainer(), '.shaka-skip-button', 'click', (el, ev) => {
				this.onSkipClicked.fire({
					forward: el.classList.contains('shaka-skip-forward-button'),
					playbackTimeMillis: this.player.getMediaElement().currentTime * 1000
				})
			}, {capture: true})
		});

		this.setUrls(config.hlsUrl, config.dashUrl);
	}

	private createUiManifest(manifest: Manifest): UiShakaManifestConfig {
		return {
			variants: manifest.variants.map(v => {
				return {
					id: v.id,
					audio: {
						id: v.audio.id,
						originalId: v.audio.originalId,
						mimeType: v.audio.mimeType,
						codecs: v.audio.codecs,
						bandwidth: v.audio.bandwidth,
						label: v.audio.label,
						roles: v.audio.roles,
						language: v.audio.language,
						channelsCount: v.audio.channelsCount,
						audioSamplingRate: v.audio.audioSamplingRate
					},
					video: {
						id: v.video.id,
						originalId: v.video.originalId,
						mimeType: v.video.mimeType,
						codecs: v.video.codecs,
						bandwidth: v.video.bandwidth,
						label: v.video.label,
						roles: v.video.roles,
						frameRate: v.video.frameRate,
						pixelAspectRatio: v.video.pixelAspectRatio,
						width: v.video.width,
						height: v.video.height
					},
					bandwidth: v.bandwidth
				}
			})
		};
	}

	private reconfigurePlayer() {
		this.player.configure({
			manifest: {
				disableVideo: this._config.videoDisabled // will advice the manifest parser to set all videos to null. Note that the quality selection button must be removed!
			},
			preferredAudioLanguage: this._config.preferredAudioLanguage
		} );
	}

	private reconfigureUi() {
		this.ui.configure({
			overflowMenuButtons: this.getOverflowMenuButtonsConfig()
		} as Partial<UIConfiguration>);
	}

	private getOverflowMenuButtonsConfig() {
		let manifest = this.player.getManifest();
		const overflowButtons = [
			"playback_rate",
			"captions",
			// "picture_in_picture",
		];
		if (manifest != null && manifest.variants.some(v => v.video != null)) {
			overflowButtons.unshift("quality");
		}
		if (manifest != null && manifest.variants.some(v => v.language && v.language !== "und")) {
			overflowButtons.unshift("language");
		}
		return overflowButtons;
	}

	public doGetMainElement(): HTMLElement {
		return this.$componentWrapper;
	}


	public play(): any {
		this.$video.play();
	}

	public pause(): any {
		this.$video.pause();
	}

	public jumpTo(timeMillis: number) {
		this.$video.currentTime = timeMillis / 1000;
	}

	public selectAudioLanguage(language: string, role: string): any {
		this._config.preferredAudioLanguage = language;
		this.player.selectAudioLanguage(language, role);
	}

	public onResize(): void {

	}

	public destroy(): void {
	}

	@executeWhenFirstDisplayed(true)
	async setUrls(hlsUrl: string, dashUrl: string) {
		const support = await shaka.Player.probeSupport();
		let url = support.manifest.mpd && dashUrl ? dashUrl : hlsUrl;
		console.log(url)
		try {
			await this.player.load(url);
			console.log(this.player.getConfiguration().preferredAudioLanguage)
		} catch (e) {
			console.error(e);
			this.onErrorLoading.fire({});
		}
	}

	static setDistinctManifestAudioTracksFixEnabled(enabled: boolean) {
		const dashParserFactory = enabled ? () => new DistinctAudioTracksManifestParserDecorator(new shaka.dash.DashParser()) : () => new shaka.dash.DashParser();
		shaka.media.ManifestParser.registerParserByExtension('mpd', dashParserFactory);
		shaka.media.ManifestParser.registerParserByMime('application/dash+xml', dashParserFactory);
		shaka.media.ManifestParser.registerParserByMime('video/vnd.mpeg.dash.mpd', dashParserFactory);
		const hlsParserFactory = enabled ? () => new DistinctAudioTracksManifestParserDecorator(new shaka.hls.HlsParser()) : () => new shaka.hls.HlsParser();
		shaka.media.ManifestParser.registerParserByExtension('m3u8', hlsParserFactory);
		shaka.media.ManifestParser.registerParserByMime('application/x-mpegurl', hlsParserFactory);
		shaka.media.ManifestParser.registerParserByMime('application/vnd.apple.mpegurl', hlsParserFactory);
	}
}

class DistinctAudioTracksManifestParserDecorator implements ManifestParser {
	constructor(private delegate: ManifestParser) {
	}

	configure(config: ManifestConfiguration) {
		return this.delegate.configure(config);
	}

	async start(uri: string, playerInterface: PlayerInterface) {
		let manifest = await this.delegate.start(uri, playerInterface);
		// Add a unique role for each audio track.
		// manifest.variants.forEach((variant, i) => variant.language = variant.language + i)
		manifest.variants.forEach((variant, i) => variant.audio.roles.push("role" + i));
		return manifest;
	}

	async stop() {
		return this.delegate.stop();
	}

	onExpirationUpdated(sessionId: string, expiration: number): any {
		return this.delegate.onExpirationUpdated(sessionId, expiration);
	}

	update(): any {
		return this.delegate.update();
	}

	banLocation(location: string): void {
		return this.delegate.banLocation(location);
	}

	onInitialVariantChosen(variant: shaka.extern.Variant): void {
		return this.delegate.onInitialVariantChosen(variant);
	}

	setMediaElement(mediaElement: HTMLMediaElement): void {
		return this.delegate.setMediaElement(mediaElement);
	}
}

shaka.polyfill.installAll();

TeamAppsUiComponentRegistry.registerComponentClass("UiShakaPlayer", UiShakaPlayer);
