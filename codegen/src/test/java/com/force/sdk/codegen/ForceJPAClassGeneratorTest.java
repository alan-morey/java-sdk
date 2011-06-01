/**
 * Copyright (c) 2011, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.force.sdk.codegen;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;

/**
 * Unit tests for {@link ForceJPAClassGenerator}.
 *
 * @author Tim Kral
 */
public class ForceJPAClassGeneratorTest {

    @DataProvider
    protected Object[][] packageNameProvider() {
        return new Object[][] {
                {"", false},
                {"1", false},
                {"..", false},
                {"com", true},
                {"1com", false},
                {"com1", true},
                {"com.deadbeef", true},
                {"com.1deadbeef", false},
                {"com.deadbeef.", false},
                {"com.deadbeef.model", true},
                {"com/deadbeef/model", false},
        };
    }
    
    @Test(dataProvider = "packageNameProvider")
    public void testIsPackageNameValid(String packageName, boolean expectedIsValid) {
        assertEquals(ForceJPAClassGenerator.isValidPackageName(packageName), expectedIsValid,
                "This package name should be " + (expectedIsValid ? "valid: " : "invalid: ") + packageName);
    }
    
    @Test
    public void testGenerateWithInvalidPackageName() throws ConnectionException, IOException {
        ForceJPAClassGenerator generator =
            new ForceJPAClassGenerator((PartnerConnection) null, new File(System.getProperty("java.io.tmpdir")));
        generator.setPackageName("1");
        
        try {
            generator.generateJPAClasses(Collections.<String>emptyList());
            fail("Code generation should have failed due to an invalid package name");
        } catch (IllegalArgumentException expected) {
            assertEquals(expected.getMessage(), "Invalid package name: 1");
        }
    }
}