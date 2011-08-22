//ID
package data_load.readers;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ResourceBundle;
import java.util.Vector;

import spade.analysis.system.GeoDataReader;
import spade.lib.lang.Language;
import spade.lib.util.CopyFile;
import spade.vis.database.DataPortion;
import spade.vis.database.DataSupplier;
import spade.vis.database.LayerData;
import spade.vis.database.SpatialEntity;
import spade.vis.dmap.DGeoLayer;
import spade.vis.dmap.DGridLayer;
import spade.vis.geometry.RasterGeometry;
import spade.vis.spec.DataSourceSpec;

/**
* Gets a raster from a ADF file
*/
public class ADFReader extends BaseDataReader implements GeoDataReader, DataSupplier {
	/**
	* The spatial data loaded
	*/
	static ResourceBundle res = Language.getTextResource("data_load.readers.Res");
	protected LayerData data = null;

	/**
	* Assuming that the data stream is already opened, tries to read from it
	* spatial raster data in FLT format
	*/
	protected LayerData readSpecific() {
		int Col, Row;
		boolean Intr = true, Geog = true;
		float Xbeg, Ybeg, DX, DY;
		float[][] ras = null;
		float minV = Float.POSITIVE_INFINITY;
		float maxV = Float.NEGATIVE_INFINITY;
		float x1, y1, x2, y2;
		String dataSource = spec.source;

		try {
			InputStream stream = null;
			DataInputStream reader;

//dblbnd.adf
			dataSource = CopyFile.getDir(dataSource) + "dblbnd.adf";
			//following text:"reading "
			showMessage(res.getString("reading") + dataSource, false);
			stream = openStream(dataSource);
			if (stream == null)
				return null;
			reader = new DataInputStream(stream);
			float D_LLX = (float) reader.readDouble();
			float D_LLY = (float) reader.readDouble();
			float D_URX = (float) reader.readDouble();
			float D_URY = (float) reader.readDouble();
			closeStream(stream);

//sta.adf
			dataSource = CopyFile.getDir(dataSource) + "sta.adf";
			//following text:"reading "
			showMessage(res.getString("reading") + dataSource, false);
			stream = openStream(dataSource);
			if (stream == null)
				return null;
			reader = new DataInputStream(stream);
			minV = (float) reader.readDouble();
			maxV = (float) reader.readDouble();
			closeStream(stream);

//hdr.adf
			dataSource = CopyFile.getDir(dataSource) + "hdr.adf";
			//following text:"reading "
			showMessage(res.getString("reading") + dataSource, false);
			stream = openStream(dataSource);
			if (stream == null)
				return null;
			reader = new DataInputStream(stream);
			reader.skip(16);
			int HCellType = reader.readInt();
			boolean floatData = (HCellType == 1) ? false : true;
			reader.skip(236);
			DX = (float) reader.readDouble();
			DY = (float) reader.readDouble();
			reader.skip(16);
			int HTilesPerRow = reader.readInt();
			reader.skip(4);
			int HTileXSize = reader.readInt();
			reader.skip(4);
			int HTileYSize = reader.readInt();
			closeStream(stream);

			Col = Math.round((D_URX - D_LLX) / DX);
			Row = Math.round((D_URY - D_LLY) / DY);

			Xbeg = D_LLX + DX / 2.0f;
			Ybeg = D_LLY + DY / 2.0f;

			ras = new float[Col][Row];
			for (int i = 0; i < Row; i++) {
				for (int j = 0; j < Col; j++) {
					ras[j][i] = Float.NaN;
				}
			}

/*
      x1=(float)Xbeg;
      y1=(float)Ybeg;
      x2=(float)(Xbeg+DX*Col);
      y2=(float)(Ybeg+DY*Row);
*/
//ID
			x1 = (Xbeg - DX / 2);
			y1 = (Ybeg - DY / 2);
			x2 = (Xbeg + DX * Col - DX / 2);
			y2 = (Ybeg + DY * Row - DY / 2);
//~ID

			System.out.println("Raster parameters:  " + "Col: " + Col + ", Row: " + Row + ", Xbeg: " + Xbeg + ", Ybeg: " + Ybeg + ", DX: " + DX + ", DY: " + DY);

/// Reading raster

//w001001x.adf
			dataSource = CopyFile.getDir(dataSource) + "w001001x.adf";
			//following text:"reading "
			showMessage(res.getString("reading") + dataSource, false);
			stream = openStream(dataSource);
			if (stream == null)
				return null;
			reader = new DataInputStream(stream);
			reader.skip(24);
			int tileCount = (reader.readInt() - 50) / 4;
			reader.skip(72);

			int[] tileOffset = new int[tileCount];
			int[] tileSize = new int[tileCount];

			for (int curr = 0; curr < tileCount; curr++) {
				tileOffset[curr] = reader.readInt() * 2;
				tileSize[curr] = reader.readInt() * 2;
			}
			closeStream(stream);

//w001001.adf
			dataSource = CopyFile.getDir(dataSource) + "w001001.adf";
			//following text:"reading "
			showMessage(res.getString("reading") + dataSource, false);

			int tileX, tileY, xx, yy;
			float curV = 0;
			byte valB = 0;
			short valS = 0;
			long valL = 0;

			for (int tile = 0; tile < tileCount; tile++) {
				if (tileSize[tile] == 0) {
					continue;
				}
				tileX = tile % HTilesPerRow;
				tileY = tile / HTilesPerRow;
				stream = openStream(dataSource);
				if (stream == null)
					return null;
				reader = new DataInputStream(stream);
				reader.skip(tileOffset[tile] + 2);

				if (floatData) {
					for (yy = tileY * HTileYSize; yy < (tileY + 1) * HTileYSize; yy++) {
						for (xx = tileX * HTileXSize; xx < (tileX + 1) * HTileXSize; xx++) {
							try {
								curV = reader.readFloat();
								if (curV < spec.validMin || curV > spec.validMax) {
									curV = Float.NaN;
								}
								if (Math.abs(curV) > 1E38f || Float.isNaN(curV) || Float.isInfinite(curV)) {
									ras[xx][Row - yy - 1] = Float.NaN;
								} else {
									ras[xx][Row - yy - 1] = curV;
								}
							} catch (Exception ex) {
								continue;
							}
						}
					}
				} else {
					byte RTileType = reader.readByte();
//System.out.println("Tile type: "+Integer.toHexString((int)RTileType & 0xFF));
					byte RMinSize = reader.readByte();
					long RMin;
					switch (RMinSize) {
					case 0:
						RMin = 0;
						break;
					case 1:
						RMin = reader.readByte();
						break;
					case 2:
						RMin = reader.readShort();
						break;
					case 4:
						RMin = reader.readInt();
						break;
					case 8:
						RMin = reader.readLong();
						break;
					//following text:"Uncompatible RMinSize value: "
					default:
						throw new IOException(res.getString("Uncompatible_RMinSize") + RMinSize);
					}

					switch (RTileType) {
					case 0x00:
						for (yy = tileY * HTileYSize; yy < (tileY + 1) * HTileYSize; yy++) {
							for (xx = tileX * HTileXSize; xx < (tileX + 1) * HTileXSize; xx++) {
								try {
									curV = RMin;
									if (curV < spec.validMin || curV > spec.validMax) {
										curV = Float.NaN;
									}
									ras[xx][Row - yy - 1] = curV;
								} catch (Exception ex) {
									continue;
								}
							}
						}
						break;
					case 0x01:
						xx = 0;
						yy = 0;
						while (yy < HTileYSize && xx * yy < HTileXSize * HTileYSize) {
							valB = reader.readByte();

							//bit 7
							try {
								curV = (RMin + (valB & 0x80) >> 7);
								if (curV < spec.validMin || curV > spec.validMax) {
									curV = Float.NaN;
								}
								ras[xx + tileX * HTileXSize][Row - 1 - (yy + tileY * HTileYSize)] = curV;
							} catch (Exception ex) {/* continue; */
							}
							xx++;
							if (xx >= HTileXSize) {
								xx -= HTileXSize;
								yy++;
							}

							//bit 6
							try {
								curV = (RMin + (valB & 0x40) >> 6);
								if (curV < spec.validMin || curV > spec.validMax) {
									curV = Float.NaN;
								}
								ras[xx + tileX * HTileXSize][Row - 1 - (yy + tileY * HTileYSize)] = curV;
							} catch (Exception ex) {/* continue; */
							}
							xx++;
							if (xx >= HTileXSize) {
								xx -= HTileXSize;
								yy++;
							}

							//bit 5
							try {
								curV = (RMin + (valB & 0x20) >> 5);
								if (curV < spec.validMin || curV > spec.validMax) {
									curV = Float.NaN;
								}
								ras[xx + tileX * HTileXSize][Row - 1 - (yy + tileY * HTileYSize)] = curV;
							} catch (Exception ex) {/* continue; */
							}
							xx++;
							if (xx >= HTileXSize) {
								xx -= HTileXSize;
								yy++;
							}

							//bit 4
							try {
								curV = (RMin + (valB & 0x10) >> 4);
								if (curV < spec.validMin || curV > spec.validMax) {
									curV = Float.NaN;
								}
								ras[xx + tileX * HTileXSize][Row - 1 - (yy + tileY * HTileYSize)] = curV;
							} catch (Exception ex) {/* continue; */
							}
							xx++;
							if (xx >= HTileXSize) {
								xx -= HTileXSize;
								yy++;
							}

							//bit 3
							try {
								curV = (RMin + (valB & 0x08) >> 3);
								if (curV < spec.validMin || curV > spec.validMax) {
									curV = Float.NaN;
								}
								ras[xx + tileX * HTileXSize][Row - 1 - (yy + tileY * HTileYSize)] = curV;
							} catch (Exception ex) {/* continue; */
							}
							xx++;
							if (xx >= HTileXSize) {
								xx -= HTileXSize;
								yy++;
							}

							//bit 2
							try {
								curV = (RMin + (valB & 0x04) >> 2);
								if (curV < spec.validMin || curV > spec.validMax) {
									curV = Float.NaN;
								}
								ras[xx + tileX * HTileXSize][Row - 1 - (yy + tileY * HTileYSize)] = curV;
							} catch (Exception ex) {/* continue; */
							}
							xx++;
							if (xx >= HTileXSize) {
								xx -= HTileXSize;
								yy++;
							}

							//bit 1
							try {
								curV = (RMin + (valB & 0x02) >> 1);
								if (curV < spec.validMin || curV > spec.validMax) {
									curV = Float.NaN;
								}
								ras[xx + tileX * HTileXSize][Row - 1 - (yy + tileY * HTileYSize)] = curV;
							} catch (Exception ex) {/* continue; */
							}
							xx++;
							if (xx >= HTileXSize) {
								xx -= HTileXSize;
								yy++;
							}

							//bit 0
							try {
								curV = (RMin + (valB & 0x01));
								if (curV < spec.validMin || curV > spec.validMax) {
									curV = Float.NaN;
								}
								ras[xx + tileX * HTileXSize][Row - 1 - (yy + tileY * HTileYSize)] = curV;
							} catch (Exception ex) {/* continue; */
							}
							xx++;
							if (xx >= HTileXSize) {
								xx -= HTileXSize;
								yy++;
							}

						}
						break;
					case 0x04:
						xx = 0;
						yy = 0;
						while (yy < HTileYSize && xx * yy < HTileXSize * HTileYSize) {
							valB = reader.readByte();

							try {
								curV = (RMin + ((valB & (short) 0xF0) >> 4));
								if (curV < spec.validMin || curV > spec.validMax) {
									curV = Float.NaN;
								}
								ras[xx + tileX * HTileXSize][Row - 1 - (yy + tileY * HTileYSize)] = curV;
							} catch (Exception ex) {/* continue; */
							}
							xx++;
							if (xx >= HTileXSize) {
								xx -= HTileXSize;
								yy++;
							}

							try {
								curV = (RMin + (valB & (short) 0x0F));
								if (curV < spec.validMin || curV > spec.validMax) {
									curV = Float.NaN;
								}
								ras[xx + tileX * HTileXSize][Row - 1 - (yy + tileY * HTileYSize)] = curV;
							} catch (Exception ex) {/* continue; */
							}
							xx++;
							if (xx >= HTileXSize) {
								xx -= HTileXSize;
								yy++;
							}
						}
						break;
					case 0x08:
						for (yy = tileY * HTileYSize; yy < (tileY + 1) * HTileYSize; yy++) {
							for (xx = tileX * HTileXSize; xx < (tileX + 1) * HTileXSize; xx++) {
								try {
									curV = (RMin + (0x000000FF & reader.readByte()));
									if (curV < spec.validMin || curV > spec.validMax) {
										curV = Float.NaN;
									}
									ras[xx][Row - yy - 1] = curV;
								} catch (Exception ex) {
									continue;
								}
							}
						}
						break;
					case 0x10:
						for (yy = tileY * HTileYSize; yy < (tileY + 1) * HTileYSize; yy++) {
							for (xx = tileX * HTileXSize; xx < (tileX + 1) * HTileXSize; xx++) {
								try {
									curV = (RMin + (0x0000FFFF & reader.readShort()));
									if (curV < spec.validMin || curV > spec.validMax) {
										curV = Float.NaN;
									}
									ras[xx][Row - yy - 1] = curV;
								} catch (Exception ex) {
									continue;
								}
							}
						}
						break;
					case (byte) 0xCF:
						// not yet implemented
/*              xx=0; yy=0;
              try {
              while (yy<HTileYSize && xx*yy < HTileXSize*HTileYSize) {
                valB = reader.readByte();
//System.out.println(valB);
                if (valB>0) {
                  for (int i=0; i<valB; i++) {
                    try {
                      curV = (float)(RMin + ((int)0x0000FFFF & (int)reader.readShort()));
//                      curV = (float)(RMin + reader.readShort());
                      if (curV < spec.validMin || curV > spec.validMax) curV = Float.NaN;
                      ras[xx+tileX*HTileXSize][Row-1-(yy+tileY*HTileYSize)] = curV;
                    }
                    catch (ArrayIndexOutOfBoundsException ex) {}
                    xx++;
                    if (xx>=HTileXSize) {
                      xx-=HTileXSize;
                      yy++;
                    }
                  }
                } else {
                  valL = reader.readByte();
                  for (int i=0; i<-valB; i++) {
                    try {
                      curV = Float.NaN;
                      if (curV < spec.validMin || curV > spec.validMax) curV = Float.NaN;
                      ras[xx+tileX*HTileXSize][Row-1-(yy+tileY*HTileYSize)] = curV;
                    }
                    catch (ArrayIndexOutOfBoundsException ex) { if (i<-valB+3) {xx=0; yy++; break;} }
                    xx++;
                    if (xx>=HTileXSize) {
                      xx-=HTileXSize;
                      yy++;
                    }
                  }
                }
              }
              }
              catch (Exception ex) {}*/
						break;
					case (byte) 0xD7:
						// not yet implemented
/*              xx=0; yy=0;
              try {
              while (yy<HTileYSize && xx*yy < HTileXSize*HTileYSize) {
                valB = reader.readByte();
//System.out.println(valB);
                if (valB>0) {
                  for (int i=0; i<valB; i++) {
                    try {
                      curV = (float)(RMin + ((int)0x000000FF & (int)reader.readByte()));
//                      curV = (float)(RMin + (0x00FF & (short)reader.readByte()));
                      if (curV < spec.validMin || curV > spec.validMax) curV = Float.NaN;
                      ras[xx+tileX*HTileXSize][Row-1-(yy+tileY*HTileYSize)] = curV;
                    }
                    catch (ArrayIndexOutOfBoundsException ex) {}
                    xx++;
                    if (xx>=HTileXSize) {
                      xx-=HTileXSize;
                      yy++;
                    }
                  }
                } else {
                  valL = reader.readByte();
                  for (int i=0; i<-valB; i++) {
                    try {
                      curV = Float.NaN;
                      if (curV < spec.validMin || curV > spec.validMax) curV = Float.NaN;
                      ras[xx+tileX*HTileXSize][Row-1-(yy+tileY*HTileYSize)] = curV;
                    }
                    catch (ArrayIndexOutOfBoundsException ex) { if (i<-valB+3) {xx=0; yy++; break;} }
                    xx++;
                    if (xx>=HTileXSize) {
                      xx-=HTileXSize;
                      yy++;
                    }
                  }
                }
              }
              }
              catch (Exception ex) {}*/
						break;
					case (byte) 0xDF:
						// not yet implemented
						break;
					case (byte) 0xE0:
						// not yet implemented
/*              xx=0; yy=0;
              try {
              while (yy<HTileYSize && xx*yy < HTileXSize*HTileYSize) {
                valS = (short)((short)0x00FF & (short)reader.readByte());
                valL = reader.readLong();
                  for (int i=0; i<valS; i++) {
                    try {
                      curV = (float)(RMin + valL);
                      if (curV < spec.validMin || curV > spec.validMax) curV = Float.NaN;
                      ras[xx+tileX*HTileXSize][Row-1-(yy+tileY*HTileYSize)] = curV;
                    }
                    catch (ArrayIndexOutOfBoundsException ex) {}
                    xx++;
                    if (xx>=HTileXSize) {
                      xx-=HTileXSize;
                      yy++;
                    }
                  }
              }
              }
              catch (Exception ex) {}*/
						break;
					case (byte) 0xF0:
						xx = 0;
						yy = 0;
						while (yy < HTileYSize && xx * yy < HTileXSize * HTileYSize) {
							valS = (short) ((short) 0x00FF & reader.readByte());
							valL = 0x0000FFFF & reader.readShort();
							for (int i = 0; i < valS; i++) {
								try {
									curV = (RMin + valL);
									if (curV < spec.validMin || curV > spec.validMax) {
										curV = Float.NaN;
									}
									ras[xx + tileX * HTileXSize][Row - 1 - (yy + tileY * HTileYSize)] = curV;
								} catch (ArrayIndexOutOfBoundsException ex) {/* continue; */
								}
								xx++;
								if (xx >= HTileXSize) {
									xx -= HTileXSize;
									yy++;
								}
							}
						}
						break;
					case (byte) 0xFC:
					case (byte) 0xF8:
						xx = 0;
						yy = 0;
						while (yy < HTileYSize && xx * yy < HTileXSize * HTileYSize) {
							valS = (short) ((short) 0x00FF & reader.readByte());
							valL = (short) 0x00FF & reader.readByte();
							for (int i = 0; i < valS; i++) {
								try {
									curV = (RMin + valL);
									if (curV < spec.validMin || curV > spec.validMax) {
										curV = Float.NaN;
									}
									ras[xx + tileX * HTileXSize][Row - 1 - (yy + tileY * HTileYSize)] = curV;
								} catch (ArrayIndexOutOfBoundsException ex) {/* continue; */
								}
								xx++;
								if (xx >= HTileXSize) {
									xx -= HTileXSize;
									yy++;
								}
							}
						}
						break;
					case (byte) 0xFF:
						// not yet implemented
						break;
					// following text:"Uncomatible RTileType value: "
					default:
						break;//throw new IOException(res.getString("Uncomatible_RTileType")+Integer.toHexString((int)RTileType & 0xFF));
					}

				}

				closeStream(stream);

			}

//~ Reading raster

// ***!!!*** abort
//        notifyProcessState("Aborting reading raster",true);
//        if (true) return null;

		} catch (IOException ioe) {
			//following text:"Exception reading raster: "
			showMessage(res.getString("Exception_reading") + ioe, true);
			return null;
		}

//for (int i=0; i<Col; i++)
//System.out.println(ras[i][Row/2] + "   ");

		LayerData data = new LayerData();
		data.setBoundingRectangle(x1, y1, x2, y2);
		RasterGeometry geom = new RasterGeometry();
		geom.ras = ras;
		geom.rx1 = x1;
		geom.ry1 = y1;
		geom.rx2 = x2;
		geom.ry2 = y2;

		geom.Col = Col;
		geom.Row = Row;
		geom.Intr = Intr;
		geom.Geog = Geog;
		geom.Xbeg = Xbeg;
		geom.Ybeg = Ybeg;
		geom.DX = DX;
		geom.DY = DY;
		geom.maxV = maxV;
		geom.minV = minV;

		SpatialEntity spe = new SpatialEntity("raster");
		spe.setGeometry(geom);
		data.addDataItem(spe);
		//following text:"The raster has been loaded from "
		showMessage(res.getString("The_raster_has_been") + spec.source, false);
		data.setHasAllData(true);
		return data;

//  System.out.println("Raster parameters:  " + Col + "  " + Row + "  " + Xbeg + "  " + Ybeg + "  " + DX + "  " + DY + "  " + Intr + "  " + Geog);
//  return null;
	}

//~ID
	/**
	* Opens the stream on the specified data source. If the source
	* string starts with HTTP or FILE, accesses the source as a URL, otherwise
	* opens it as a local file
	*/
	protected InputStream openStream(String dataSource) {
		if (dataSource == null)
			return null;
		int idx = dataSource.indexOf(':');
		boolean isURL = false;
		if (idx > 0) {
			String pref = dataSource.substring(0, idx);
			if (pref.equalsIgnoreCase("HTTP") || pref.equalsIgnoreCase("FILE")) {
				isURL = true;
			}
		}
		InputStream stream = null;
		try {
			if (isURL) { //try to access the source as a URL
				URL url = new URL(dataSource);
				URLConnection urlc = url.openConnection();
				urlc.setUseCaches(mayUseCache);
				stream = urlc.getInputStream();
			} else {
				stream = new FileInputStream(dataSource);
			}
		} catch (IOException ioe) {
			//following text:"Error accessing "
			showMessage(res.getString("Error_accessing") + dataSource + ": " + ioe, true);
			return null;
		}
		return stream;
	}

	protected void closeStream(InputStream stream) {
		if (stream == null)
			return;
		try {
			stream.close();
		} catch (IOException ioe) {
		}
	}

	/**
	* Loads the data. When allowed (the argument mayAskUser is true) and necessary,
	* starts a dialog or a wizard for the user
	* to specify the data source, constraints, etc. Returns true if the data
	* have been successfully loaded.
	*/
	@Override
	public boolean loadData(boolean mayAskUser) {
		if (dataError)
			return false;
		if (dataReadingInProgress) {
			waitDataReadingFinish();
			return !dataError;
		}
		if (!mayAskUser) {
			setDataReadingInProgress(true);
		}
		if (spec == null || spec.source == null)
			if (mayAskUser) {
				//following text:"Select the file with raster data"
				String path = browseForFile(res.getString("Select_the_file_with"), "*.adf");
				if (path == null) {
					setDataReadingInProgress(false);
					return false;
				}
				if (spec == null) {
					spec = new DataSourceSpec();
				}
				spec.source = path;
			} else {
				//following text:"The data source for layer is not specified!"
				showMessage(res.getString("The_data_source_for"), true);
				setDataReadingInProgress(false);
				return false;
			}
		if (spec.name == null) {
			spec.name = CopyFile.getName(spec.source);
		}
		//following text:"Start reading raster data from "
		showMessage(res.getString("Start_reading_raster") + spec.source, false);
		data = readSpecific();
		dataError = data == null;
		setDataReadingInProgress(false);
		return !dataError;
	}

	/**
	* Returns the map layer constructed from the geographical data loaded (if any).
	* If the data have not been loaded yet, the reader constructs a layer without
	* objects and sets itself as the  data supplier of the layer. Then the data
	* will be loaded when the layer is first darwn.
	*/
	@Override
	public DGeoLayer getMapLayer() {
		DGridLayer layer = new DGridLayer();
		layer.setDataSource(spec);
		if (spec.id != null) {
			layer.setContainerIdentifier(spec.id);
		}
		if (spec.name != null) {
			layer.setName(spec.name);
		} else if (spec.source != null) {
			layer.setName(CopyFile.getName(spec.source));
		}
		if (data != null) {
			layer.receiveSpatialData(data);
		} else {
			layer.setDataSupplier(this);
		}
		return layer;
	}

//----------------- DataSupplier interface -----------------------------------
	/**
	* Returns the SpatialDataPortion containing all DataItems available
	*/
	@Override
	public DataPortion getData() {
		if (data != null)
			return data;
		if (dataError)
			return null;
		if (loadData(false))
			return data;
		return null;
	}

	/**
	* Constructs and returns a DataPortion containing DataItems fitting in at
	* least one of the specified bounding rectangles (listed in the vector).
	* The elements of the vector are instances of
	* @see spade.vis.geometry.RealRectangle
	* Readers from files do not filter data according to any query,
	* therefore the method getData() without arguments is called
	*/
	@Override
	public DataPortion getData(Vector bounds) {
		return getData();
	}

	/**
	* When no more data from the DataSupplier are needed, this method is
	* called. Here the DataSupplier can clear its internal structures
	*/
	@Override
	public void clearAll() {
		data = null;
	}
}
