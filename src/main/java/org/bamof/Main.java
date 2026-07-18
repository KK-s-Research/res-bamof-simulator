package org.bamof;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.bamof.experiment.ExperimentRunner;
import org.bamof.model.RunResult;

public final class Main {
	private Main() {

	}

	public static void main(String[] args) throws IOException {
		boolean full = !hasArg(args, "quick");
		System.out.println("BAMOF simulator");
		System.out.println("Mode: " + (full ? "full manuscript experiments" : "quick smoke experiments"));
		System.out.println("Algorithms: " + ExperimentRunner.algorithms());
		List<RunResult> results = new ExperimentRunner().run(full);
		System.out.println("Completed simulation runs: " + results.size());
		System.out.println("Outputs written to " + (full ? "results/manuscript-run/" : "results/quick-run/"));
		if (full) {
			System.out.println("Run with argument 'quick' only for a reduced smoke-test configuration.");
		}
	}

	private static boolean hasArg(String[] args, String expected) {
		return Arrays.stream(args).anyMatch(arg -> expected.equalsIgnoreCase(arg));
	}
}
