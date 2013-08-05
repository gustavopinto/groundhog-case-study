package br.ufpe.cin.groundhog.answers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import br.ufpe.cin.groundhog.License;
import br.ufpe.cin.groundhog.Project;
import br.ufpe.cin.groundhog.User;
import br.ufpe.cin.groundhog.answers.utils.Projects;
import br.ufpe.cin.groundhog.codehistory.GitCodeHistory;
import br.ufpe.cin.groundhog.crawler.CrawlGitHub;
import br.ufpe.cin.groundhog.crawler.ForgeCrawler;
import br.ufpe.cin.groundhog.http.HttpModule;
import br.ufpe.cin.groundhog.parser.license.LicenseParser;
import br.ufpe.cin.groundhog.scmclient.GitClient;
import br.ufpe.cin.groundhog.search.SearchGitHub;
import br.ufpe.cin.groundhog.search.SearchModule;
import br.ufpe.cin.groundhog.util.FileUtil;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class Issues {

	static File downloadFolder = FileUtil.getInstance().createTempDir();
	
	static Injector injector = Guice.createInjector(new SearchModule(),
			new HttpModule());
	static SearchGitHub searchGitHub = injector.getInstance(SearchGitHub.class);

	static ForgeCrawler crawler = new CrawlGitHub(
			injector.getInstance(GitClient.class), downloadFolder);
	
	static GitCodeHistory codeHistory = injector.getInstance(GitCodeHistory.class);
	
	/**
	 * Testing issue 22 - https://github.com/spgroup/groundhog/issues/22
	 * 
	 * How commonplace are project forks?
	 * 
	 * Algorithm:
	 * 
	 * Search: the first two projects, of the first page for projects named
	 * "opencv" get ratio: get the number of the projects returned from the
	 * search above that are forks.
	 */
	public static void issue22() throws IOException {

		int n = 1, m = 15;
		List<Project> projects = searchGitHub.getProjects("opencv", 1, 15);

		System.out.println("Searching GitHub for 'opencv': the first " + m
				+ " results of the " + n + " page...");
		System.out.println(projects.size() + " projects returned");
		double ratio = Projects.getProjectsThatAreForks(projects);

		System.out.println("Are forks: " + ratio);

		/*
		 * If we want - hypothetically - to find out how many projects on GitHub
		 * are forks List<Project> projects = searchGitHub.getProjects(1, -1);
		 * Beware, this will zero your API limit!
		 * 
		 * Testing issue 22 - getProjectsWithForksRate Find out how many have
		 * forks
		 */

		double nratio = Projects.getProjectsWithForksRate(projects);
		System.out.println("Have forks: " + nratio);

		int aratio = Projects.getAverageForksRate(projects);
		System.out
				.println("Average number of forks between the searched projects: "
						+ aratio);

		// Fun with Milestones:

		User spg = new User("spgroup");
		Project pr = new Project(spg, "groundhog");

		pr.setMilestones(searchGitHub.getAllProjectMilestones(pr));

		System.out.println("Searching for Milestones...");

		for (int h = 0; h < pr.getMilestones().size(); h++) {
			System.out.println(pr.getMilestones().get(h).getTitle());
		}

		// Fun with Issues:
		pr.setIssues(searchGitHub.getAllProjectIssues(pr));

		System.out.println("Searching for Issues...");
		for (int k = 0; k < pr.getIssues().size(); k++) {
			System.out.println(pr.getIssues().get(k).getTitle());
		}
	}

	/**
	 * Testing issue 47 - https://github.com/spgroup/groundhog/issues/47
	 * 
	 * How many projects are created each year?
	 * 
	 * Algorithm:
	 * 
	 * Search for a number of java projects, and organize the information in the
	 * createdat column.
	 */
	public static void issue47() {
		
		System.out.println("downloading projects..");
		List<Project> projects = searchGitHub.getAllProjects(0, 2);
		Map<Integer, Integer> years = new HashMap<Integer, Integer>();

		Calendar c = Calendar.getInstance();

		for (Project project : projects) {
			c.setTime(project.getCreatedAt());
			int year = c.get(Calendar.YEAR);

			Integer value = years.get(year);
			if (value == null) {
				years.put(year, 1);
			} else {
				years.put(year, value++);
			}
		}

		System.out.println("The result is:");
		System.out.println(years);
	}

	/**
	 * Testing issue 56: https://github.com/spgroup/groundhog/issues/56
	 * 
	 * To provide an answer to the question "What are the five most used licenses?"
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 * 
	 */
	public static void issue56() throws InterruptedException, ExecutionException {
		
		System.out.println("searching...");
		List<Project> projects = searchGitHub.getAllProjects(0, 5);
		
		System.out.print("download... ");
		System.out.println("they will be at " + downloadFolder.getAbsolutePath());
		
		List<Future<File>> futures = crawler.downloadProjects(projects);
		List<License> licenses = new ArrayList<>();
		for (Future<File> f : futures) { // wait for download
			File repositoryFolder = f.get();
			
			System.out.println("checkouting project " + repositoryFolder.getName());
			Date date = new GregorianCalendar(2013, 6, 1).getTime();
			File temp = codeHistory.checkoutToDate(repositoryFolder.getName(), repositoryFolder, date);
			
			License l = new LicenseParser(temp).parser();
			licenses.add(l);
		}
		
		System.out.println(licenses);
	}
}
