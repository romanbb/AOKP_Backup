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

    public String toString() {
        return setting + "=" + val;
    }
}