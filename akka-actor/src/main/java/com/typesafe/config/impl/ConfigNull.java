/**
 *   Copyright (C) 2011 Typesafe Inc. <http://typesafe.com>
 */
package com.typesafe.config.impl;

import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigValueType;

/**
 * This exists because sometimes null is not the same as missing. Specifically,
 * if a value is set to null we can give a better error message (indicating
 * where it was set to null) in case someone asks for the value. Also, null
 * overrides values set "earlier" in the search path, while missing values do
 * not.
 *
 */
final class ConfigNull extends AbstractConfigValue {

    ConfigNull(ConfigOrigin origin) {
        super(origin);
    }

    @Override
    public ConfigValueType valueType() {
        return ConfigValueType.NULL;
    }

    @Override
    public Object unwrapped() {
        return null;
    }

    @Override
    String transformToString() {
        return "null";
    }
}
