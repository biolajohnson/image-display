import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;

public class ImageDisplay {

	JFrame frame;
	JLabel lbIm1;
	BufferedImage imageOne;
	int width = 512; // default image width and height
	int height = 512;
	int numOfBits = 8; // default image number of bits
	Map<Integer, Integer> map;
	Map<Integer, Integer> upperTable;
	Map<Integer, Integer> lowerTable;
	int intervalGap;
	int pivot;


	public ImageDisplay(int numBits, int mode) {
		if (mode == -1) {
			map = new HashMap<>();
			int interval = (int) Math.pow(2, numBits);
			this.intervalGap = (int) (256 / interval);
			int avg = intervalGap / 2;
			for (int i = 0; i < interval; i++) {
				map.put(i, (i * intervalGap) + avg); 
			}
		} else {
			upperTable = new HashMap<>();
			lowerTable = new HashMap<>();
			this.pivot = mode;
			int levels = (int) Math.pow(2, numOfBits);
			int interval = (int) Math.pow(2, numBits);
			this.intervalGap = (int) (256 / interval);
			// upper table
			int upper = (pivot / 256) * levels;
			this.intervalGap = (int) log(intervalGap);
			for (int i = 0; i < upper; i++) {
			upperTable.put(i, (intervalGap * 2) + pivot);
		}
			// lower table
			int lower = levels - upper;
			for (int i = 0; i < lower; i++) {
			lowerTable.put(i, pivot - (intervalGap * 2));
		}

		}
	
	}

	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	private BufferedImage readImageRGB(int width, int height, String imgPath, BufferedImage img)
	{
		try
		{
			int frameLength = width * height*3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);

			int ind = 0;
			for(int y = 0; y < height; y++)
			{
				for (int x = 0; x < width; x++) {
					// byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind + height * width];
					byte b = bytes[ind + height * width * 2];
					

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					
					img.setRGB(x, y, pix);
					ind++;
				}
			}
			raf.close();
			
		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		return img;
	}

	public void showIms(String[] args) {

		// Read a parameter from command line
		String imagePath = args[0];
		double scale = Double.parseDouble(args[1]);
		int numOfBits = Integer.parseInt(args[2]);
		int mode = Integer.parseInt(args[3]);
	

		System.out.println("Mode: " + mode);

		// Read in the specified image
		imageOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, imagePath, imageOne);

		// Sub sample image using the number of bits and scale
		if (numOfBits < this.numOfBits) {
			// Apply low pass filter (blur)
			lowPass(imageOne);

			// perform sub sampling
		  imageOne = subSample(numOfBits, imageOne, scale, mode);
		}

		// Use label to display the image
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		lbIm1 = new JLabel(new ImageIcon(imageOne));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbIm1, c);

		frame.pack();
		frame.setVisible(true);
	}

	// sub sample code
	public BufferedImage subSample(int numOfBits, BufferedImage image, double scale, int mode) {
		
		int width = (int) (this.width * scale);
		int height = (int) (this.height * scale);

		// create new buffer image to be returned
		BufferedImage imageTwo = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		
		// Loop through the buffer image
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				int x = (int) (i / scale);
				int y = (int) (j / scale);
		
				int rgb = image.getRGB(x, y);
				Color color = new Color(rgb);
				// quantize each rgb color 
				if (mode == -1) {
					color = quantize(color, numOfBits);
				} else {
					color = logQuantize(mode, color, numOfBits);
				}
				
				rgb = color.getRGB();
				imageTwo.setRGB(i, j, rgb);

			}
		}
		return imageTwo;
	}

	public Color logQuantize(int mode, Color color, int intervalGap) {
		int red = color.getRed();
		int green = color.getGreen();
		int blue = color.getBlue();
		red = logQuantizeHelper(red);
		green = logQuantizeHelper(green);
		blue = logQuantizeHelper(blue);

		return new Color(red, green, blue);
	}

	public  Color quantize(Color color, double numBits) {
		int red = color.getRed();
		int green = color.getGreen();
		int blue = color.getBlue();
		red = quantizeHelper(red);
		green = quantizeHelper(green);
		blue = quantizeHelper(blue);
		return new Color(red, green, blue);
	}
	
	/** Calculate the color value in a logoarithmic scale per num of bits */

	public int logQuantizeHelper(int color) {
		if (color > this.pivot) {
			
			color = upperTable.get(color);
		} else {
			color = 
			color = lowerTable.get(color);
		}
		return color;
	}
	
	public int quantizeHelper(int color) {
		color = color / this.intervalGap;
		color = this.map.get(color);
		return color;
	}

	public static BufferedImage copy(BufferedImage source) {

		ColorModel colorModel = source.getColorModel();
		WritableRaster raster = source.copyData(null);
		boolean isAlphaPreMultiplied = source.isAlphaPremultiplied();
		return new BufferedImage(colorModel, raster, isAlphaPreMultiplied, null);
	}

	public static BufferedImage lowPass(BufferedImage image) {

		int height = image.getHeight();
		int width = image.getWidth();
		int x_coords, y_coords;
		Color color = new Color(0, 0, 0);
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				int r = 0, g = 0, b = 0;
				for (int x = -1; x < 2; x++) {
					for (int y = -1; y < 2; y++) {
						x_coords = i + x;
						y_coords = j + y;
						if (x_coords < 0 || y_coords < 0 || x_coords >= height || y_coords >= width) {
							continue;
						}
						int rgb = (image.getRGB(x_coords, y_coords));
						Color c = new Color(rgb);
						r += (int) (c.getRed() * 1f / 9f);
						g += (int) (c.getGreen() * 1f / 9f);
						b += (int) (c.getBlue() * 1f / 9f);

					}
				}
				color = new Color(r, g, b);
				image.setRGB(i, j, color.getRGB());
			}
		}
		return image;
	}
	
	/** Helper function for logarithm calculations */
	public static double log(double num){
			return Math.log(num) / Math.log(2);
	}

	public static void main(String[] args) {
		int numBits = Integer.parseInt(args[2]);
		int mode = Integer.parseInt(args[3]);
		ImageDisplay ren = new ImageDisplay(numBits, mode);
		ren.showIms(args);
	}

}
