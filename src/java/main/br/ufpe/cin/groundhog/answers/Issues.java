package br.ufpe.cin.groundhog.answers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;

import br.ufpe.cin.groundhog.Commit;
import br.ufpe.cin.groundhog.GroundhogException;
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
import br.ufpe.cin.groundhog.search.SearchException;
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
		List<Project> projects = searchGitHub.getAllForgeProjects(10, 30);

		System.out.print("Downloading projects... ");
		System.out.println("they will be available at "
				+ downloadFolder.getAbsolutePath());
		HashMap<String, Integer> licenses = new HashMap<>();

		try {
			for (Project project : projects) {
				File projectLocal = crawler.downloadProject(project);

				Set<License> ls = new LicenseParser(projectLocal).parser();
				
				License l = ls.iterator().next();
				
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
	
	/**
	 * Testing issue 53 - https://github.com/spgroup/groundhog/issues/53
	 * 
	 * How did the no. of commits for Java projects change over years?
	 * 
	 * Algorithm:
	 * 
	 */
	public static void issue53() {
		List<Project> projects = searchGitHub.getAllProjectsByLanguage("java");
		
		System.out.println(projects.size() + " projects will be analized..");
		for (Project project : projects) {
			List<Commit> commits = searchGitHub.getAllProjectCommits(project);

			System.out.println("Listing commits from project " + project.getName());
			for (Commit commit : commits) {
				System.out.println(commit.getSha() + "-" + commit.getCommitDate());
			}
		}
	}
	
	/**
	 * Testing issue 48 - https://github.com/spgroup/groundhog/issues/48 
	 * 
	 * To provide an answer to the question "How many Java projects were active in 2012?
	 * 
	 */
	public static void issue48() {
		try {
			int numberOfProjects = 100;
			
			List<Project> rawData = searchGitHub.getAllProjects(0, numberOfProjects);

			List<Project> projects = new ArrayList<>();

			for (Project project : rawData) {

				List<Commit> commits = searchGitHub.getAllProjectCommitsByDate(project, "2012-01-01", "2012-12-31");

				System.out.println("Project Name: "+project.getName());
				System.out.println("Number of commits: "+commits.size());

				if(commits.size() > 0){
					projects.add(project);
				}
				
				System.out.println("Number of projects with more than one commit: "+projects.size());
				
				double percent = ((projects.size()*1.0)/numberOfProjects)*100.0;
				System.out.println( percent + "% of the projects java of " + numberOfProjects + " projects");
			}

		} catch (GroundhogException e) {
			e.printStackTrace();
			throw new SearchException(e);
		}
	}
	
	/**
	 * Testing issue 49 - https://github.com/spgroup/groundhog/issues/49
	 * 
	 * To provide an answer to the question "How many revisions are there in all Java projects?"
	 * 
	 */
	public static void issue49() {
		try {
			
			int response = 0;
			int numberTotalTags = 0;
			
			List<Project> projects = searchGitHub.getAllProjects(0, 5000);
			
			for (Iterator<Project> iterator = projects.iterator(); iterator.hasNext();) {
				
				Project project = (Project) iterator.next();
				
				response = searchGitHub.getNumberProjectTags(project);
				
				System.out.println("Project Name: "+project.getName());
				System.out.println("Number of tags: "+response);
				
				if(response > 0){
					
					numberTotalTags += response;
				}
				
				response = 0;
			}
			
			float average = (float) ((numberTotalTags*(1.0))/(projects.size()*(1.0)));
			
			System.out.println("In " + projects.size() + " java projects there are " + numberTotalTags + " tags");
			System.out.println("An average of " + average + " tags per project");
			
		} catch (GroundhogException e) {
			e.printStackTrace();
			throw new SearchException(e);
		}
	}
}
