package br.ufpe.cin.groundhog.answers;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	static GitCodeHistory codeHistory = injector
			.getInstance(GitCodeHistory.class);

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
	 * Testing issue 56: https://github.com/spgroup/groundhog/issues/56
	 * 
	 * To provide an answer to the question
	 * "What are the five most used licenses?"
	 * 
	 */
	public static void issue56() throws Exception {

		System.out.println("Searching...");
		List<Project> projects = searchGitHub.getAllForgeProjects(0, 100);

		System.out.print("Downloading projects... ");
		System.out.println("they will be available at "
				+ downloadFolder.getAbsolutePath());
		HashMap<String, Integer> licenses = new HashMap<>();

		try {
			for (Project project : projects) {
				File projectLocal = crawler.downloadProject(project);

				Date date = new GregorianCalendar(2013, 6, 1).getTime();
				File temp = codeHistory.checkoutToDate(projectLocal.getName(),
						projectLocal, date);

				License l = new LicenseParser(temp).parser();
				
				Integer value = licenses.get(l.getName());
				if (value == null) {
					licenses.put(l.getName(), 1);
				} else {
					licenses.put(l.getName(), ++value);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println(licenses);
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
		Map<Integer, Integer> years = new HashMap<>();
		Map<String, Integer> langs = new HashMap<>();

		System.out.println("Download projects..");
		List<Project> projects = searchGitHub.getAllProjects(1, 900);
		for (Project project : projects) {
			Date created_at = project.getCreatedAt();
			Calendar c = Calendar.getInstance();
			c.setTime(created_at);
			Integer year = c.get(Calendar.YEAR);

			Integer value = years.get(year);
			if (value == null) {
				years.put(year, 1);
			} else {
				years.put(year, ++value);
			}

			String lang = project.getLanguage();
			value = langs.get(lang);
			if (value == null) {
				langs.put(lang, 1);
			} else {
				langs.put(lang, ++value);
			}
		}

		System.out.println("The total of projects created each year is:");
		System.out.println("years: " + years);
		System.out.println("languages: " + langs);
	}
}
