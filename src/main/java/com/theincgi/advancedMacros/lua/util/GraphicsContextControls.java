package com.theincgi.advancedMacros.lua.util;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import org.luaj.vm2_v3_0_1.LuaError;
import org.luaj.vm2_v3_0_1.LuaTable;
import org.luaj.vm2_v3_0_1.LuaValue;
import org.luaj.vm2_v3_0_1.Varargs;
import org.luaj.vm2_v3_0_1.lib.VarArgFunction;

import com.theincgi.advancedMacros.AdvancedMacros;
import com.theincgi.advancedMacros.gui.Color;
import com.theincgi.advancedMacros.misc.CallableTable;
import com.theincgi.advancedMacros.misc.Utils;

public class GraphicsContextControls extends LuaTable{
	BufferedImage img;
	BufferedImageControls bic;
	Graphics g;
	public GraphicsContextControls(BufferedImageControls bi) {
		bic = bi;
		img = bi.img;
		g = img.getGraphics();

		loadFuncs();

		//setColor
		//draw/fill rect, 

		//destroy
	}


	private void loadFuncs() {
		for(Draw op : Draw.values()) {
			String[] docLocation = op.getDocLocation();
			this.set(op.name(), new CallableTable(docLocation, new DrawOp(op)));
		}
	}

	public class DrawOp extends VarArgFunction {
		Draw op;
		public DrawOp(Draw op) {
			this.op = op;
		}
		@Override
		public Varargs invoke(Varargs args) {
			switch(op) {
			//case getFontMetrics:
			//  throw new LuaError("Unimplemented");
			case drawImage:{
				LuaValue v = args.arg1();
				if(v instanceof BufferedImageControls) {
					BufferedImageControls bic = (BufferedImageControls) v;
					args = args.subargs(2);
					switch (args.narg()) {
					case 2:
						g.drawImage(bic.getImg(), args.arg(1).checkint()-1, args.arg(2).checkint()-1, null);
						return NONE;
						
					case 4:
					case 6:
					case 8:
						int destX = args.checkint(1)-1;
						int destY = args.checkint(2)-1;
						int destW = args.checkint(3)-destX;
						int destH = args.checkint(4)-destY;

						int srcX = args.optint(5, 1) - 1;
						int srcY = args.optint(6, 1) - 1;
						int maxW = bic.getImg().getWidth() - srcX + 1;
						int maxH = bic.getImg().getHeight() - srcY + 1;
						int srcW = args.optint(7, maxW) - srcX;
						int srcH = args.optint(8, maxH) - srcY;
						
						g.drawImage(bic.getImg(), destX, destY,
												  destW, destH,
												  srcX,  srcY,
												  srcW,  srcH, null);  //source x, y 2
						return NONE;
					
					default:
						throw new LuaError("Unexpected arguments, expected (pic, x, y, [wid, hei [, srcX, srcY [,srcWid, srcHei]])]");
					}
				}else {
					throw new LuaError("arg 1 is not a usable image");
				}
			}
			case destroy: //aka dispose
				g.dispose();
				bic.getLuaValTexture().deleteTex();
				return NONE;

			case setColor: {
				g.setColor(Utils.parseColor(args, AdvancedMacros.COLOR_SPACE_IS_255).toAWTColor());
				return NONE;
			}
			case translate:
				g.translate(args.checkint(1), args.checkint(2));//TODO 1 index?
				return NONE;

			case clipRect:
				g.clipRect(args.checkint(1)-1, args.checkint(2)-1, //x, y
						args.checkint(3)-1, args.checkint(4)-1); //width, height
				return NONE;
			case getColor:
				return new Color(g.getColor().getRGB()).toLuaValue(AdvancedMacros.COLOR_SPACE_IS_255);

			case setPaintMode:
				g.setPaintMode();
				return NONE;

			case setXORMode:
				g.setXORMode(Utils.parseColor(args,AdvancedMacros.COLOR_SPACE_IS_255).toAWTColor());
				return NONE;

			case getFont:{
				Font f = g.getFont();
				LuaTable out = new LuaTable();
				out.set("name", f.getFamily());
				out.set("size", f.getSize());
				out.set("bold",   LuaValue.valueOf(f.isBold()));
				out.set("italic", LuaValue.valueOf(f.isItalic()));
				return out; //TODO font measurments
			}
			case setFont:{
				//TODO table arg and vararg name, <size>, <bold>, <italic>
				Font f = null;
				switch(args.narg()) {
				case 1:
					if(args.istable(1)) {
						LuaTable t = args.arg1().checktable();
						int flag = t.get("bold").optboolean(false)? Font.BOLD : Font.PLAIN;
						flag = t.get("italic").optboolean(false)? flag | Font.ITALIC : flag;
						f = new Font(t.get("name").checkjstring(), flag, t.get("size").optint(12)); //default size 12
						throw new LuaError("Unimplemented"); //TODO
					}else{
						f = Font.getFont(args.checkjstring(1));
					}
					break;
				case 2:
					f = new Font(args.checkjstring(1), Font.PLAIN, args.checkint(2)); //name, size
					break;
				case 3:
				case 4:
					int flag = args.optboolean(3, false)? Font.BOLD : Font.PLAIN;
					flag = args.optboolean(4, false)? flag | Font.ITALIC : flag;
					f = new Font(args.checkjstring(1), flag, args.checkint(2)); //name, size
				default:
					throw new LuaError("Unexpected number of arguments");
				}
				g.setFont(f);
				return NONE;
			}
			case getFonts:{
				LuaTable fonts = new LuaTable();
				GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

				for (Font font : ge.getAllFonts()) {
					if(!font.getFontName().toLowerCase().contains("italic") && !font.getFontName().toLowerCase().contains("bold"))
						fonts.set(fonts.length()+1, font.getFontName());
				}
				return fonts;
			}
			//case getClipRect:
			//case getClipBounds:
			case getClip:{
				Rectangle rect = g.getClipBounds();
				if(rect == null) return FALSE;
				LuaTable out = new LuaTable();
				out.set(1, LuaValue.valueOf(rect.x     +1));
				out.set(2, LuaValue.valueOf(rect.y     +1));
				out.set(3, LuaValue.valueOf(rect.width   ));
				out.set(4, LuaValue.valueOf(rect.height  ));
				return out.unpack();
			}

			case setClip:{
				if(args.arg1().isnil() || args.arg1().isboolean() && args.arg1().checkboolean()==false) {
					g.setClip(null);
					return NONE;
				}
				Rectangle rect = new Rectangle(args.checkint(1)-1, args.checkint(2)-1, args.checkint(3), args.checkint(4)); //x,y,wid,hei
				g.setClip(rect);
				return NONE;
			}

			case copyArea:
				g.copyArea(args.checkint(1)-1, args.checkint(2)-1, //x, y
						args.checkint(3), args.checkint(4), //width, height
						args.checkint(5)-1, args.checkint(6)-1);//dx, dy
				return NONE;

			case drawLine:
				g.drawLine(args.checkint(1)-1, args.checkint(2)-1, //x1, y1
						args.checkint(3)-1, args.checkint(4)-1);//x2, y1
				return NONE;

			case fillRect:
				g.fillRect(args.checkint(1)-1, args.checkint(2)-1,  //x, y
						args.checkint(3), args.checkint(4)); //width, height
				return NONE;

			case drawRect:
				g.drawRect(args.checkint(1)-1, args.checkint(2)-1,  //x, y
						args.checkint(3), args.checkint(4)); //width, height
				return NONE;

			case clearRect:
				g.clearRect(args.checkint(1)-1, args.checkint(2)-1,  //x, y
						args.checkint(3), args.checkint(4)); //width, height
				return NONE;

			case drawRoundRect:
				g.drawRoundRect(args.checkint(1)-1, args.checkint(2)-1, //x, y
						args.checkint(3), args.checkint(4), //width, height
						args.checkint(5), args.checkint(6));//arcWidth, arcHeight
				return NONE;

			case fillRoundRect:
				g.fillRoundRect(args.checkint(1)-1, args.checkint(2)-1, //x, y
						args.checkint(3), args.checkint(4), //width, height
						args.checkint(5), args.checkint(6));//arcWidth, arcHeight
				return NONE;

			case draw3DRect:
				g.draw3DRect(args.checkint(1)-1, args.checkint(2)-1,  //x, y
						args.checkint(3), args.checkint(4), //width, height
						args.checkboolean(5)); //raised);
				return NONE;

			case fill3DRect:
				g.fill3DRect(args.checkint(1)-1, args.checkint(2)-1,  //x, y
						args.checkint(3), args.checkint(4), //width, height
						args.checkboolean(5)); //raised);
				return NONE;

			case drawOval:
				g.drawOval(args.checkint(1)-1, args.checkint(2)-1,  //x, y
						args.checkint(3), args.checkint(4)); //width, height
				return NONE;

			case fillOval:
				g.fillOval(args.checkint(1)-1, args.checkint(2)-1,  //x, y
						args.checkint(3), args.checkint(4)); //width, height
				return NONE;

			case drawArc:
				g.drawArc(args.checkint(1)-1, args.checkint(2)-1, //x, y
						args.checkint(3), args.checkint(4), //width, height
						args.checkint(5), args.checkint(6));//arcStart, arcEnd
				return NONE;

			case fillArc:
				g.fillArc(args.checkint(1)-1, args.checkint(2)-1, //x, y
						args.checkint(3), args.checkint(4), //width, height
						args.checkint(5), args.checkint(6));//arcStart, arcEnd
				return NONE;
			case drawPolyline: {
				int[] xPoints, yPoints;
				LuaTable points = args.checktable(1);
				xPoints = new int[points.length()];
				yPoints = new int[points.length()];
				for(int i = 1; i <= points.length(); i++) {
					xPoints[i-1] = points.get(i).get(1).checkint()-1;
					yPoints[i-1] = points.get(i).get(2).checkint()-1;
				}
				g.drawPolyline(xPoints, yPoints, xPoints.length);
				return NONE;
			}
			case drawPolygon: {
				int[] xPoints, yPoints;
				LuaTable points = args.checktable(1);
				xPoints = new int[points.length()];
				yPoints = new int[points.length()];
				for(int i = 1; i <= points.length(); i++) {
					xPoints[i-1] = points.get(i).get(1).checkint()-1;
					yPoints[i-1] = points.get(i).get(2).checkint()-1;
				}
				g.drawPolygon(new Polygon(xPoints, yPoints, xPoints.length));
				return NONE;
			}
			case fillPolygon:{
				int[] xPoints, yPoints;
				LuaTable points = args.checktable(1);
				xPoints = new int[points.length()];
				yPoints = new int[points.length()];
				for(int i = 1; i <= points.length(); i++) {
					xPoints[i-1] = points.get(i).get(1).checkint()-1;
					yPoints[i-1] = points.get(i).get(2).checkint()-1;
				}
				g.fillPolygon(new Polygon(xPoints, yPoints, xPoints.length));
				return NONE;
			}	
			case drawString:
				g.drawString(args.checkjstring(1), args.checkint(2)-1, args.checkint(3)-1);
				return NONE;
				//			case drawChars:
				//				throw new LuaError("Unimplemented");

				//			case drawBytes:
				//				throw new LuaError("Unimplemented");



				//			case hitClip:
				//				throw new LuaError("Unimplemented");
				//			case equals:
				//			  throw new LuaError("Unimplemented");
				//			case hashCode:
				//				return LuaValue.valueOf(img.hashCode());
			default:
				throw new LuaError("Undefined action for opperation '"+op.name()+"'");
			}
		}
	}
	public static enum Draw {
		//getFontMetrics, 
		drawImage, 
		//		hashCode, 
		destroy, 
		//		hitClip, 
		//getClipRect, 
		//		drawBytes, 
		//		drawChars, 
		drawString, 
		fillPolygon, 
		drawPolygon, 
		drawPolyline, 
		fillArc, 
		setColor, 
		translate, 
		clipRect, 
		getColor, 
		setPaintMode, 
		setXORMode, 
		drawLine, 
		copyArea, 
		clearRect, 
		fillRoundRect, 
		draw3DRect, 
		fill3DRect, 
		drawOval, 
		drawArc, 
		fillOval, 
		getFont, 
		setFont, 
		//getClipBounds, 
		setClip, 
		getClip, 
		fillRect, 
		drawRect, 
		drawRoundRect
		//equals, 
		, getFonts;

		public String[] getDocLocation() {
			//image.new.___
			String[] arr = new String[3];
			arr[0] = "image";
			arr[1] = "new()";
			switch (this) {
			case getFonts:
				arr[2] = "getFonts";
				return arr;
			case clearRect:
			case clipRect:
			case copyArea:
			case destroy:
			case draw3DRect:
			case drawArc:
			case drawImage:
			case drawLine:
			case drawOval:
			case drawPolygon:
			case drawPolyline:
			case drawRect:
			case drawRoundRect:
			case drawString:
			case fill3DRect:
			case fillArc:
			case fillOval:
			case fillPolygon:
			case fillRect:
			case fillRoundRect:
			case getClip:
			case getColor:
			case getFont:
			
			case setClip:
			case setColor:
			case setFont:
			case setPaintMode:
			case setXORMode:
			case translate:
			default:
				return null;
			}
		}
	}
}
