package ufl.cs1.controllers;

import game.controllers.DefenderController;
import game.models.*;
import java.util.*;

public final class StudentController implements DefenderController
{
	public void init(Game game) { }

	public void shutdown(Game game) { }

	public int follower(Defender defender, Attacker attacker)
	{
		return defender.getNextDir(attacker.getLocation(), true);
	}

	public int coward(Defender defender, Defender bigBrother, Attacker attacker)
	{
		if(defender.getLocation().getPathDistance(bigBrother.getLocation()) > 25)
			return follower(defender, attacker);

		return defender.getNextDir(bigBrother.getLocation(), false);
	}

	public static List<Node> findJunctionsToGo(Node target, Defender defender, Attacker attacker, Node previousNode){
		List<Node> junctions = new ArrayList<Node>();
		if (target.isJunction()){
			junctions.add(target);
		}
		else{
			List<Node> neighborsList=target.getNeighbors();
			ArrayList<Node> neighbors=new ArrayList<>(neighborsList);
			for (int i=0;i<neighbors.size();i++) {
				if (neighbors.get(i)==null){
					neighbors.remove(i);
					i--;
				}
			}
			for (int i=0;i<neighbors.size();i++)
				if(neighbors.get(i).equals(previousNode))
					neighbors.remove(i);
			for(Node nextNode:neighbors)
				junctions.addAll(findJunctionsToGo(nextNode,defender, attacker, target));
		}
		return junctions;
	}

	public int[] wall(Defender defender1,Defender defender2, Defender defender3, Game game)
	{
		int[] output=new int[3];
		if(defender1.isVulnerable()){
			output[0]=escape(defender1,game.getAttacker());
		}
		else{
			List<Node> targetJunctions1=findJunctionsToGo(game.getAttacker().getLocation(),defender1,game.getAttacker(),game.getAttacker().getLocation().getNeighbor(game.getAttacker().getReverse()));
			if (targetJunctions1.get(0).equals(defender1.getLocation())){
				output[0]=follower(defender1,game.getAttacker());
			}
			else
				output[0]=defender1.getNextDir(targetJunctions1.get(0),true);
		}

		if(defender2.isVulnerable()){
			output[1]=escape(defender2,game.getAttacker());
		}
		else{
			List<Node> targetJunctions2=findJunctionsToGo(game.getAttacker().getLocation(),defender2,game.getAttacker(),game.getAttacker().getLocation().getNeighbor(game.getAttacker().getReverse()));
			if (targetJunctions2.get(0).equals(defender2.getLocation())){
				output[1]=follower(defender2,game.getAttacker());
			}
			else
				output[1]=defender1.getNextDir(targetJunctions2.get(0),true);
		}

		if(defender3.isVulnerable()){
			output[2]=escape(defender3,game.getAttacker());
		}
		else{
			List<Node> targetJunctions3=findJunctionsToGo(game.getAttacker().getLocation(),defender3,game.getAttacker(),game.getAttacker().getLocation().getNeighbor(game.getAttacker().getReverse()));
			if (targetJunctions3.get(0).equals(defender3.getLocation())){
				output[2]=follower(defender3,game.getAttacker());
			}
			else
				output[2]=defender1.getNextDir(targetJunctions3.get(0),true);
		}
		return output;
	}

	public Defender closestToTheNode(List<Defender> defenders, Node node){
		int minDistance=Integer.MAX_VALUE;
		Defender output=null;
		for(Defender d:defenders){
			int temp=d.getLocation().getPathDistance(node);
			if (temp<minDistance){
				minDistance=temp;
				output=d;
			}
		}
		return output;
	}

	public int escape(Defender d, Attacker a){
		return d.getNextDir(a.getLocation(),false);
	}

	public Node getClosestPowerPill(Game g)
	{
		List<Node> powerPills=g.getPowerPillList();
		if (powerPills.size()==0)
			return null;
		for(int i=0;i<powerPills.size()-1;i++){
			for (int j=i+1;j<powerPills.size();j++){
				if (g.getAttacker().getLocation().getPathDistance(powerPills.get(i))> g.getAttacker().getLocation().getPathDistance(powerPills.get(j))){
					Collections.swap(powerPills,i,j);
				}
			}
		}
		return powerPills.get(0);
	}

	public Node getNode(Game g, int x, int y){
		List<Node> allNodesList=g.getCurMaze().getPillNodes();
		ArrayList<Node> allNodes=new ArrayList<>(allNodesList);
		allNodes.addAll(g.getCurMaze().getJunctionNodes());
		allNodes.addAll(g.getCurMaze().getPowerPillNodes());
		for(Node n:allNodes){
			if(n.getX()==x && n.getY()==y){
				return n;
			}
		}
		return allNodes.get(0);
	}

	public int[] update(Game game,long timeDue)
	{
		//get four corners
		Maze m=game.getCurMaze();
		List<Node> allNodesList=m.getPillNodes();
		ArrayList<Node> allNodes=new ArrayList<>(allNodesList);
		allNodes.addAll(game.getCurMaze().getJunctionNodes());
		allNodes.addAll(game.getCurMaze().getPowerPillNodes());
		int minX=Integer.MAX_VALUE;
		int minY=Integer.MAX_VALUE;
		int maxX=Integer.MIN_VALUE;
		int maxY=Integer.MIN_VALUE;
		for(Node n:allNodes){
			if (n.getX()<minX)
				minX=n.getX();
		}
		for(Node n:allNodes){
			if (n.getX()>maxX)
				maxX=n.getX();
		}
		for(Node n:allNodes){
			if (n.getY()<minY)
				minY=n.getY();
		}
		for(Node n:allNodes){
			if (n.getY()>maxY)
				maxY=n.getY();
		}


		int[] actions = new int[Game.NUM_DEFENDER];
		List<Defender> enemies = game.getDefenders();
		actions[0] = follower(enemies.get(0), game.getAttacker());
		Node closestPowerPill=getClosestPowerPill(game);
		boolean ghostBeforePowerpill=false;
		if (closestPowerPill!=null) {
			List<Node> pathAttackerToPowerPill=game.getAttacker().getPathTo(closestPowerPill);
			for (Node n : pathAttackerToPowerPill) {
				if (n.equals(enemies.get(0).getLocation()) ||
						n.equals(enemies.get(1).getLocation()) ||
						n.equals(enemies.get(2).getLocation()) ||
						n.equals(enemies.get(3).getLocation())) {
					ghostBeforePowerpill = true;
					break;
				}
			}
			int quarterOfAttacker; //top left=6;top right=8; bottom left=3; bottom right=4
			int index1;
			if (game.getAttacker().getLocation().getX()<maxX/2)
				index1=3;
			else
				index1=4;
			int index2;
			if(game.getAttacker().getLocation().getY()<maxY/2)
				index2=2;
			else
				index2=1;
			quarterOfAttacker=index1*index2;
			if(!ghostBeforePowerpill&&pathAttackerToPowerPill.size()<=20){
				switch (quarterOfAttacker) {
					case 6:
						actions[1] = enemies.get(1).getNextDir(getNode(game, maxX,minY),true);
						actions[2] = enemies.get(2).getNextDir(getNode(game, minX,maxY),true);
						actions[3] = enemies.get(3).getNextDir(getNode(game, maxX,maxY),true);
						return actions;
					case 8:
						actions[1] = enemies.get(1).getNextDir(getNode(game, minX,minY),true);
						actions[2] = enemies.get(2).getNextDir(getNode(game, minX,maxY),true);
						actions[3] = enemies.get(3).getNextDir(getNode(game, maxX,maxY),true);
						return actions;
					case 3:
						actions[1] = enemies.get(1).getNextDir(getNode(game, maxX,minY),true);
						actions[2] = enemies.get(2).getNextDir(getNode(game, minX,minY),true);
						actions[3] = enemies.get(3).getNextDir(getNode(game, maxX,maxY),true);
						return actions;
					case 4:
						actions[1] = enemies.get(1).getNextDir(getNode(game, maxX,minY),true);
						actions[2] = enemies.get(2).getNextDir(getNode(game, minX,maxY),true);
						actions[3] = enemies.get(3).getNextDir(getNode(game, minX,minY),true);
						return actions;
				}
			}
		}
		int[] wallArray=wall(enemies.get(1),enemies.get(2),enemies.get(3),game);
		actions[1]=wallArray[0];
		actions[2]=wallArray[1];
		actions[3]=wallArray[2];
		return actions;
	}
}