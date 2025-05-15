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
export class SkipForwardButton extends shaka.ui.Element {

  private button:HTMLButtonElement;

  constructor(parent : HTMLElement , controls : shaka.ui.Controls) {
    super(parent, controls);

    this.button = document.createElement('button');
    this.button.classList.add('material-icons-round');
    this.button.classList.add('shaka-skip-button', 'shaka-skip-forward-button');
    this.button.textContent = "skip_next";
    this.parent.appendChild(this.button);
    this.button.setAttribute("aria-label", "Skip forward");

    this.eventManager.listen(this.button, 'click', () => {
      // nothing to do here... (this must be handled elsewhere)
    });
  }
}

export class SkipForwardButtonFactory {
  create(rootElement : HTMLElement , controls : shaka.ui.Controls) {
    return new SkipForwardButton(rootElement, controls);
  }
}

shaka.ui.Controls.registerElement('skip_forward', new SkipForwardButtonFactory());
