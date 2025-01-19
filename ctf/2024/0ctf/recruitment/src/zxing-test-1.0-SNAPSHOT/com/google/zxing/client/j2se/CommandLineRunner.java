package com.google.zxing.client.j2se;

import com.beust.jcommander.JCommander;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class CommandLineRunner {
   private CommandLineRunner() {
   }

   public static void main(String[] args) throws Exception {
      DecoderConfig config = new DecoderConfig();
      JCommander jCommander = new JCommander(config);
      jCommander.parse(args);
      jCommander.setProgramName(CommandLineRunner.class.getSimpleName());
      if (config.help) {
         jCommander.usage();
      } else {
         List<URI> inputs = new ArrayList<>(config.inputPaths.size());

         for (String inputPath : config.inputPaths) {
            URI uri;
            try {
               uri = new URI(inputPath);
            } catch (URISyntaxException var12) {
               if (!Files.exists(Paths.get(inputPath))) {
                  throw var12;
               }

               uri = new URI("file", inputPath, null);
            }

            inputs.add(uri);
         }

         do {
            inputs = retainValid(expand(inputs), config.recursive);
         } while (config.recursive && isExpandable(inputs));

         int numInputs = inputs.size();
         if (numInputs == 0) {
            jCommander.usage();
         } else {
            Queue<URI> syncInputs = new ConcurrentLinkedQueue<>(inputs);
            int numThreads = Math.min(numInputs, Runtime.getRuntime().availableProcessors());
            int successful = 0;
            if (numThreads > 1) {
               ExecutorService executor = Executors.newFixedThreadPool(numThreads);
               Collection<Future<Integer>> futures = new ArrayList<>(numThreads);

               for (int x = 0; x < numThreads; x++) {
                  futures.add(executor.submit(new DecodeWorker(config, syncInputs)));
               }

               executor.shutdown();

               for (Future<Integer> future : futures) {
                  successful += future.get();
               }
            } else {
               successful += new DecodeWorker(config, syncInputs).call();
            }

            if (!config.brief && numInputs > 1) {
               System.out.println("\nDecoded " + successful + " files out of " + numInputs + " successfully (" + successful * 100 / numInputs + "%)\n");
            }
         }
      }
   }

   private static List<URI> expand(Iterable<URI> inputs) throws IOException {
      List<URI> expanded = new ArrayList<>();

      for (URI input : inputs) {
         if (isFileOrDir(input)) {
            Path inputPath = Paths.get(input);
            if (Files.isDirectory(inputPath)) {
               try (DirectoryStream<Path> childPaths = Files.newDirectoryStream(inputPath)) {
                  for (Path childPath : childPaths) {
                     expanded.add(childPath.toUri());
                  }
               }
            } else {
               expanded.add(input);
            }
         } else {
            expanded.add(input);
         }
      }

      for (int i = 0; i < expanded.size(); i++) {
         URI inputx = expanded.get(i);
         if (inputx.getScheme() == null) {
            expanded.set(i, Paths.get(inputx.getRawPath()).toUri());
         }
      }

      return expanded;
   }

   private static List<URI> retainValid(Iterable<URI> inputs, boolean recursive) {
      List<URI> retained = new ArrayList<>();

      for (URI input : inputs) {
         boolean retain;
         if (isFileOrDir(input)) {
            Path inputPath = Paths.get(input);
            retain = !inputPath.getFileName().toString().startsWith(".") && (recursive || !Files.isDirectory(inputPath));
         } else {
            retain = true;
         }

         if (retain) {
            retained.add(input);
         }
      }

      return retained;
   }

   private static boolean isExpandable(Iterable<URI> inputs) {
      for (URI input : inputs) {
         if (isFileOrDir(input) && Files.isDirectory(Paths.get(input))) {
            return true;
         }
      }

      return false;
   }

   private static boolean isFileOrDir(URI uri) {
      return "file".equals(uri.getScheme());
   }
}
