package br.ufpe.cin.groundhog.answers;

import java.io.IOException;
import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;

import br.ufpe.cin.groundhog.Project;
import br.ufpe.cin.groundhog.User;
import br.ufpe.cin.groundhog.http.HttpModule;
import br.ufpe.cin.groundhog.search.SearchGitHub;
import br.ufpe.cin.groundhog.search.SearchModule;

public class Issues {
	
	static Injector injector = Guice.createInjector(new SearchModule(), new HttpModule());
	static SearchGitHub searchGitHub = injector.getInstance(SearchGitHub.class);
	
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
	public static void issue22() throws IOException{
		
		int n = 1, m = 15;
		List<Project> projects = searchGitHub.getProjects("opencv", 1, 15);
		
		System.out.println("Searching GitHub for 'opencv': the first " + m + " results of the " + n + " page...");
		System.out.println(projects.size() + " projects returned");
		double ratio = Projects.getProjectsThatAreForks(projects);
		
		System.out.println("Are forks: " + ratio);
		
		/*
		 * If we want - hypothetically - to find out how many projects on GitHub are forks
		 * List<Project> projects = searchGitHub.getProjects(1, -1);
		 * Beware, this will zero your API limit!
		 * 
		 * Testing issue 22 - getProjectsWithForksRate
		 * Find out how many have forks
		 */
		
		double nratio = Projects.getProjectsWithForksRate(projects);
		System.out.println("Have forks: " + nratio);
		
		int aratio = Projects.getAverageForksRate(projects);
		System.out.println("Average number of forks between the searched projects: " + aratio);
		
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
}
