/* ###
 * IP: GHIDRA
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
package ugo;

import java.io.File;

import ghidra.app.decompiler.*;
import ghidra.app.plugin.processors.sleigh.SleighLanguage;
import ghidra.program.model.lang.*;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Program;
import ghidra.util.task.TaskMonitor;
import ugo.lang.UgoDecompInterface;

class UgoDecompiler {

    private DecompInterface cachedDecompInterface;
    private DecompileOptions options;
    private int timeout;
    private volatile boolean optionsChanged = false;

    UgoDecompiler(DecompileOptions options, int timeout) {
        this.options = options;
        this.timeout = timeout;
    }

    void setOptions(DecompileOptions options) {
        this.options = options;

        // note: we have made the decision for now to allow the GUI decompiler to work for as
        //       long as it needs to, allowing the user to cancel as desired.
        // this.timeout = options.getDefaultTimeout();
        optionsChanged = true;
    }

    DecompileResults decompile(Program program, Function function, File debugFile,
                               TaskMonitor monitor) throws DecompileException {
        DecompInterface ifc = getDecompilerInterface(program, monitor);

        if (debugFile != null) {
            ifc.enableDebug(debugFile);
        }
        if (optionsChanged) {
            ifc.setOptions(options);
            optionsChanged = false;
        }
        return ifc.decompileFunction(function, timeout, monitor);
    }

    synchronized void cancelCurrentAction() {
        if (cachedDecompInterface != null) {
            cachedDecompInterface.stopProcess();
            cachedDecompInterface.dispose();
            cachedDecompInterface = null;
        }
    }

    synchronized DecompInterface getDecompilerInterface(Program program, TaskMonitor monitor) throws DecompileException {
        if (cachedDecompInterface != null) {
            if (cachedDecompInterface.getProgram() == program) {
                return cachedDecompInterface;
            }
            cachedDecompInterface.dispose();
        }
        DecompInterface newInterface = new UgoDecompInterface();
        newInterface.setOptions(options);
        optionsChanged = false;
        try {
            program.setLanguage(new SleighLanguage(
                    new SleighLanguageDescription(
                            new LanguageID("golang"),
                            "Go language",
                            Processor.toProcessor("go"), // TODO: make and instantiate an instance of a Processor and then put it in
                            Endian.LITTLE,
                            Endian.LITTLE,
                            4,
                            "unknown",
                            11, //TODO: parse this from file
                            1, // TODO: parse this from file
                            false,
                            null,
                            null,
                            null
                    )), new CompilerSpecID("go"), false, monitor);
        } catch (Exception e) {

        }
//		newInterface.toggleSyntaxTree(false);
        if (!newInterface.openProgram(program)) {
            String errorMessage = newInterface.getLastMessage();
            throw new DecompileException("Decompiler",
                    "Unable to initialize the " + "DecompilerInterface: " + errorMessage);
        }
        cachedDecompInterface = newInterface;
        return newInterface;
    }

    synchronized void dispose() {
        cancelCurrentAction();
    }

    /**
     * Resets the native decompiler process.  Call this method when the decompiler's view
     * of a program has been invalidated, such as when a new overlay space has been added.
     */
    public void resetDecompiler() {
        if (cachedDecompInterface != null) {
            cachedDecompInterface.resetDecompiler();
        }
    }

}
