package org.jasper.jLib.metpx.decoder;

import java.io.Serializable;

public class MetPxBoard implements Serializable{
	
	private static final long serialVersionUID = -4040589394251984025L;
	private Bulletin[] board;
	
	public MetPxBoard(Bulletin[] board){
		this.board = board;
	}
	
	public Bulletin[] getBoard(){
		return board;
	}
	
	public void setBoard(Bulletin[] board){
		this.board = board;
	}

}
