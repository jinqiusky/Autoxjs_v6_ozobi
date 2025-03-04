/**
 *  Copyright 2014 Ryszard Wi'sniewski <brut.alll@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package zhao.arsceditor.ResDecoder.data.value;

import android.util.TypedValue;

import java.io.IOException;

/**
 * @author Ryszard Wi'sniewski <brut.alll@gmail.com>
 */
public class ResIntValue extends ResScalarValue {
	protected final int mValue;
	private int type;

	public ResIntValue(int value, String rawValue, int type) {
		this(value, rawValue, "integer");
		this.type = type;
	}

	public ResIntValue(int value, String rawValue, String type) {
		super(type, value, rawValue);
		this.mValue = value;
	}

	@Override
	protected String encodeAsResValue() throws IOException {
		return TypedValue.coerceToString(type, mValue);
	}

	public int getValue() {
		return mValue;
	}
}