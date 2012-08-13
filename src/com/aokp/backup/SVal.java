/*
 * Copyright (C) 2012 Roman Birg
 *
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
 */

package com.aokp.backup;

public class SVal {
    String setting;
    String val;
    
    private boolean secureSetting;

    public SVal(String setting, String val) {
        this.setting = setting;
        this.val = val;
        
        if(setting.startsWith("secure.")) {
            secureSetting = true;
        }
    }
    
    public boolean isSecure() {
        return secureSetting || setting.startsWith("secure.");
    }
    
    public String getRealSettingString() {
        if(secureSetting)
            return setting.substring(7);
        else
            return setting;
    }
    
    public String getVal() {
        return val;
    }

    public String toString() {
        return setting + "=" + val;
    }
}