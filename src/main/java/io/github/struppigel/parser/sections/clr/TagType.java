/**
 * *****************************************************************************
 * Copyright 2022 Karsten Hahn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ****************************************************************************
 */
package io.github.struppigel.parser.sections.clr;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.github.struppigel.parser.sections.clr.CLRTableType.*;

public enum TagType {

    TYPEDEF_OR_REF(2, new CLRTableType[]{ CLRTableType.TYPEDEF, CLRTableType.TYPEREF, CLRTableType.TYPESPEC }),
    HAS_CONSTANT(2, new CLRTableType[]{ CLRTableType.FIELD, CLRTableType.PARAM, CLRTableType.PROPERTY }),
    HAS_CUSTOM_ATTRIBUTE(5, new CLRTableType[]{ CLRTableType.METHODDEF, CLRTableType.FIELD, CLRTableType.TYPEREF,
            CLRTableType.TYPEDEF, CLRTableType.PARAM, CLRTableType.INTERFACEIMPL, CLRTableType.MEMBERREF,
            // TODO The null type here is actually PERMISSION (p.274) --> dunno what that is! Not in spec
            CLRTableType.MODULE, null, CLRTableType.PROPERTY, CLRTableType.EVENT,
            CLRTableType.STANDALONESIG, CLRTableType.MODULEREF, CLRTableType.TYPESPEC, CLRTableType.ASSEMBLY,
            CLRTableType.ASSEMBLYREF, CLRTableType.FILE, CLRTableType.EXPORTEDTYPE, CLRTableType.MANIFESTRESOURCE,
            CLRTableType.GENERICPARAM, CLRTableType.GENERICPARAMCONSTRAINT, CLRTableType.METHODSPEC }),
    HAS_FIELD_MARSHAL(1, new CLRTableType[]{ CLRTableType.FIELD, CLRTableType.PARAM }),
    HAS_DECL_SECURITY(2, new CLRTableType[]{ CLRTableType.TYPEDEF, CLRTableType.METHODDEF, CLRTableType.ASSEMBLY }),
    MEMBERREF_PARENT(3, new CLRTableType[]{ CLRTableType.TYPEDEF, CLRTableType.TYPEREF, CLRTableType.MODULEREF,
            CLRTableType.METHODDEF, CLRTableType.TYPESPEC }),
    HAS_SEMANTICS(1, new CLRTableType[]{ CLRTableType.EVENT, CLRTableType.PROPERTY }),
    METHODDEF_OR_REF(1, new CLRTableType[]{ CLRTableType.METHODDEF, CLRTableType.MEMBERREF }),
    MEMBER_FORWARDED(1, new CLRTableType[]{ CLRTableType.FIELD, CLRTableType.METHODDEF }),
    IMPLEMENTATION(2, new CLRTableType[]{ CLRTableType.FILE, CLRTableType.ASSEMBLYREF, CLRTableType.EXPORTEDTYPE }),
            // these null types are not set, but must be filled in for the tag access to work
    CUSTOM_ATTRIBUTE_TYPE(3, new CLRTableType[]{ null, null, CLRTableType.METHODDEF, CLRTableType.MEMBERREF, null }),
    RESOLUTION_SCOPE(2, new CLRTableType[]{ CLRTableType.MODULE, CLRTableType.MODULEREF, CLRTableType.ASSEMBLYREF,
            CLRTableType.TYPEREF }),
    TYPE_OR_METHODDEF(1, new CLRTableType[]{ CLRTableType.TYPEDEF, CLRTableType.METHODDEF });

    private final int size;
    private final CLRTableType[] tables;

    private static final Logger logger = LogManager
            .getLogger(TagType.class.getName());

    private TagType(int size, CLRTableType[] tables) {
        this.size = size;
        this.tables = tables;
    }

    /**
     * Size in bits to encode a tag of this type
     * @return size in bits
     */
    public int getSize() {
        return size;
    }

    public Optional<CLRTableType> getTableForTag(int tag) {
        try {
            return Optional.ofNullable(tables[tag]);
        } catch (ArrayIndexOutOfBoundsException e){
            return Optional.empty();
        }
    }

    public CLRTableType[] getAllTables() {
        return tables;
    }

    public static Optional<TagType> getTagTypeForCLRTableKey(CLRTableKey key) {
        Map<CLRTableKey, TagType> m = new HashMap<>();
        m.put(CLRTableKey.CONSTANT_PARENT, HAS_CONSTANT);
        m.put(CLRTableKey.CUSTOMATTRIBUTE_TYPE, CUSTOM_ATTRIBUTE_TYPE);
        m.put(CLRTableKey.CUSTOMATTRIBUTE_PARENT, HAS_CUSTOM_ATTRIBUTE);
        m.put(CLRTableKey.DECLSECURITY_PARENT, HAS_DECL_SECURITY);
        m.put(CLRTableKey.EVENT_EVENTTYPE, TYPEDEF_OR_REF);
        m.put(CLRTableKey.EXPORTEDTYPE_IMPLEMENTATION, IMPLEMENTATION);
        m.put(CLRTableKey.FIELDMARSHAL_PARENT, HAS_FIELD_MARSHAL);
        m.put(CLRTableKey.GENERICPARAM_OWNER, TYPE_OR_METHODDEF);
        m.put(CLRTableKey.GENERICPARAMCONSTRAINT_CONSTRAINT, TYPEDEF_OR_REF);
        m.put(CLRTableKey.IMPLMAP_MEMBERFORWARDED, MEMBER_FORWARDED);
        m.put(CLRTableKey.INTERFACEIMPL_INTERFACE, TYPEDEF_OR_REF);
        m.put(CLRTableKey.MANIFESTRESOURCE_IMPLEMENTATION, IMPLEMENTATION);
        m.put(CLRTableKey.MEMBERREF_CLASS, MEMBERREF_PARENT);
        m.put(CLRTableKey.METHODIMPL_METHODBODY, METHODDEF_OR_REF);
        m.put(CLRTableKey.METHODIMPL_METHODDECLARATION, METHODDEF_OR_REF);
        m.put(CLRTableKey.METHODSEMANTICS_ASSOCIATION, HAS_SEMANTICS);
        m.put(CLRTableKey.METHODSPEC_METHOD, METHODDEF_OR_REF);
        m.put(CLRTableKey.TYPEDEF_EXTENDS, TYPEDEF_OR_REF);
        m.put(CLRTableKey.TYPEREF_RESOLUTION_SCOPE, RESOLUTION_SCOPE);
        return Optional.ofNullable(m.get(key));
    }
}
