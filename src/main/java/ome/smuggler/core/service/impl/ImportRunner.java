package ome.smuggler.core.service.impl;

import static java.util.Objects.requireNonNull;
import static ome.smuggler.core.msg.ChannelMessage.message;
import static ome.smuggler.core.msg.RepeatAction.Repeat;
import static ome.smuggler.core.msg.RepeatAction.Stop;
import static ome.smuggler.core.service.impl.Loggers.logTransientError;

import java.io.IOException;
import java.nio.file.Path;

import ome.smuggler.core.msg.RepeatAction;
import ome.smuggler.core.service.ImportProcessor;
import ome.smuggler.core.types.FutureTimepoint;
import ome.smuggler.core.types.ImportLogFile;
import ome.smuggler.core.types.QueuedImport;


public class ImportRunner implements ImportProcessor {

    private final ImportEnv env;
    
    public ImportRunner(ImportEnv env) {
        requireNonNull(env, "env");
        this.env = env;
    }
    
    private int run(ImporterCommandBuilder cliOmeroImporter, QueuedImport task) 
            throws IOException, InterruptedException {
        CommandRunner runner = new CommandRunner(cliOmeroImporter);
        Path importTarget = env.importLogPathFor(task.getTaskId()).get(); 
        return runner.exec(importTarget);
    }
    
    private void scheduleDeletion(QueuedImport task) {
        ImportLogFile logFile = env.importLogPathFor(task.getTaskId()).file();
        FutureTimepoint when = env.importLogRetentionFromNow();
        env.gcQueue().uncheckedSend(message(when, logFile));
    }
    
    @Override
    public RepeatAction consume(QueuedImport task) {
        ImporterCommandBuilder cliOmeroImporter = 
                new ImporterCommandBuilder(env.cliConfig(), task.getRequest());
        ImportOutput output = new ImportOutput(
                env.importLogPathFor(task.getTaskId()), task);
        
        try {
            output.writeHeader(cliOmeroImporter);
            int status = run(cliOmeroImporter, task); 
            boolean succeeded = status == 0;
            
            output.writeFooter(succeeded, status);
            return succeeded ? Stop : Repeat;
        } catch (IOException | InterruptedException e) {
            output.writeFooter(e);
            logTransientError(this, e);
            return Repeat;
        } finally {
            scheduleDeletion(task);
        }
    }

}
