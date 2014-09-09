/**
 *
 * Lattes2Endnote:
 * Lattes XML to EndNote tab format reference extractor and converter.
 *
 * Copyright (C) 2014 Dilvan A. Moreira
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * The author can be contacted at dilvan@gmail.com
 *
 */

package org.dilvan.lattes2endnote

import static groovy.transform.TypeCheckingMode.*
import groovy.swing.SwingBuilder
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked

import java.awt.Component
import java.awt.Insets

import javax.swing.BorderFactory
import javax.swing.ImageIcon
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.border.Border
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.*

import org.w3c.dom.Node
import org.w3c.dom.NodeList


/*
 EndNote tag Reference
 EndNote Reference Types
 %0 Generic
 %0 Artwork
 %0 Audiovisual Material
 %0 Bill
 %0 Book
 %0 Book Section
 %0 Case
 %0 Chart or Table
 %0 Classical Work
 %0 Computer Program
 %0 Conference Paper
 %0 Conference Proceedings
 %0 Edited Book
 %0 Equation
 %0 Electronic Article
 %0 Electronic Book
 %0 Electronic Source
 %0 Figure
 %0 Film or Broadcast
 %0 Government Document
 %0 Hearing
 %0 Journal Article
 %0 Legal Rule/Regulation
 %0 Magazine Article
 %0 Manuscript
 %0 Map
 %0 Newspaper Article
 %0 Online Database
 %0 Online Multimedia
 %0 Patent
 %0 Personal Communication
 %0 Report
 %0 Statute
 %0 Thesis
 %0 Unpublished Work
 %0 Unused 1
 %0 Unused 2
 %0 Unused
 .
 EndNote tag format:
 %A Author
 %B Secondary Title (of a Book or Conference Name)
 %C Place Published
 %D Year
 %E Editor /Secondary Author
 %F Label
 %G Language
 %H Translated Author
 %I Publisher
 %J Secondary Title (Journal Name)
 %K Keywords
 %L Call Number
 %M Accession Number
 %N Number (Issue)
 %P Pages
 %Q Translated Title
 %R Electronic Resource Number
 %S Tertiary Title
 %T Title
 %U URL
 %V Volume
 %W Database Provider
 %X Abstract
 %Y Tertiary Author
 %Z Notes
 %0 Reference Type
 %1 Custom 1
 %2 Custom 2
 %3 Custom 3
 %4 Custom 4
 %6 Number of Volumes
 %7 Edition
 %8 Date
 %9 Type of Work
 %? Subsidiary Author
 %@ ISBN/ISSN
 %! Short Title
 %# Custom 5
 %$ Custom 6
 %] Custom 7
 %& Section
 %( Original Publication
 %) Reprint Edition
 %* Reviewed Item
 %+ Author Address
 %^ Caption
 %> Link to PDF
 %< Research Notes
 %[ Access Date
 %= Last Modified Date
 %~ Name of Database
 */

/**
 * Lattes2Endnotes: It converts publication references from the Lattes xml format
 * to the EndNote format
 * Lattes is a format used by the Lattes <a href="http://lattes.cnpq.br/">Lattes</a>
 * tool (from CNPq Brazil)
 * EndNote is a format used by the <a href="http://endnote.com/">EndNote reference manager tool
 * </a> (from Thomson Reuters)
 *
 * @author Dilvan Moreira
 * @date 2014-09-08
 *
 */
@CompileStatic
class Lattes2Endnote {

	int year1, year2

	def baseKind = [
		['%0 Journal Article', '//ARTIGO-PUBLICADO'],
		['%0 Conference Paper', '//TRABALHO-EM-EVENTOS'],
		['%0 Book', '//LIVRO-PUBLICADO'],
		['%0 Book Section','//CAPITULO-DE-LIVRO-PUBLICADO'],
		['%0 Newspaper Article','//TEXTO-EM-JORNAL-OU-REVISTA[DADOS-BASICOS-DO-TEXTO/@NATUREZA="JORNAL_DE_NOTICIAS"]'],
		['%0 Magazine Article','//TEXTO-EM-JORNAL-OU-REVISTA[DADOS-BASICOS-DO-TEXTO/@NATUREZA="REVISTA_MAGAZINE"]']
	]

	def kind = baseKind

	def conv = [
		['%A ', './/@NOME-COMPLETO-DO-AUTOR'],
		['%B ', './/@NOME-DO-EVENTO'],
		['%B ', './/@TITULO-DO-LIVRO'],
		['%C ', './/@LOCAL-DE-PUBLICACAO'],
		['%C ', './/@CIDADE-DA-EDITORA'],
		['%D ', './/@ANO-DO-ARTIGO'],
		['%D ', './/@ANO-DO-TRABALHO'],
		['%D ', './/@ANO'],
		['%D ', './/@ANO-DO-TEXTO'],
		['%E ', './/@ORGANIZADORES'],
		['%G ', './/@IDIOMA'],
		['%I ', './/@NOME-DA-EDITORA'],
		['%J ', './/@TITULO-DO-PERIODICO-OU-REVISTA'],
		['%J ', './/@TITULO-DO-JORNAL-OU-REVISTA'],
		['%K ', './/PALAVRAS-CHAVE/@*'],
		['%N ', './/@FASCICULO'],
		['%P ', './/@PAGINA-INICIAL', '-', './/@PAGINA-FINAL'],
		['%Q ', './/@TITULO-DO-ARTIGO-INGLES'],
		['%Q ', './/@TITULO-DO-TRABALHO-INGLES'],
		['%Q ', './/@TITULO-DO-CAPITULO-DO-LIVRO-INGLES'],
		['%Q ', './/@TITULO-DO-TEXTO-INGLES'],
		['%R ', './/@DOI'],
		['%T ', './/@TITULO-DO-ARTIGO'],
		['%T ', './/@TITULO-DO-TRABALHO'],
		['%T ', './/@TITULO-DO-CAPITULO-DO-LIVRO'],
		['%T ', './/@TITULO-DO-TEXTO'],
		['%U ', './/@HOME-PAGE-DO-TRABALHO'],
		['%V ', './/@VOLUME'],
		// It's last to avoid problematic characters inside the notes
		// Commented out as it can generate new lines
		//['%Z ', './/@DESCRICAO-INFORMACOES-ADICIONAIS'],
		['%6 ', './/@NUMERO-DE-VOLUMES'],
		['%7 ', './/@NUMERO-DA-EDICAO-REVISAO'],
		['%8 ', './/@DATA-DE-PUBLICACAO'],
		['%@ ', './/@ISSN'],
		['%@ ', './/@ISBN']
	]

	def inPeriod(XPath xpath, Node pub){
		try {
			def cmd = './/@ANO-DO-ARTIGO | .//@ANO-DO-TRABALHO | .//@ANO | .//@ANO-DO-TEXTO'
			def yearStr = ((NodeList) xpath.evaluate(cmd, pub, XPathConstants.NODESET)).item(0).nodeValue

			def year = Integer.parseInt(yearStr)
			(year >= year1) && (year <= year2)
		}
		catch (e) {false}
	}

	def path (String fileName, String fileOutput){

		def domFactory = DocumentBuilderFactory.newInstance()
		domFactory.namespaceAware = true
		def doc = domFactory.newDocumentBuilder().parse(fileName)
		def xpath = XPathFactory.newInstance().newXPath()

		new File(fileOutput).withWriter { out ->

			kind.each{ List<String> type ->
				xpath.evaluate(type[1], doc, XPathConstants.NODESET ).each{ Node pub ->
					if (!inPeriod(xpath, pub)) return;
					out.writeLine(type[0])
					conv.each{ List<String> cmd ->
						xpath.evaluate(cmd[1], pub, XPathConstants.NODESET).each{ Node node ->
							if (node.nodeValue=='') return;
							out.write(cmd[0] + node.nodeValue)
							//print cmd[0] + '--'+ node.nodeValue+'\n'

							// Two columns command?
							if (cmd.size()>2)
								xpath.evaluate(cmd[3], pub, XPathConstants.NODESET).each{ Node it ->
									out.write(cmd[2] + it.nodeValue)
								}
							out.write('\n')
						}
					}
					out.write('\n')
				}
			}
		}
	}

	@TypeChecked(SKIP)
	def setPreferences(journals, conferences, books, chapters, newspapers, magazines, from, to){
		kind = []
		if (journals) kind.add(baseKind[0])
		if (conferences) kind.add(baseKind[1])
		if (books) kind.add(baseKind[2])
		if (chapters) kind.add(baseKind[3])
		if (newspapers) kind.add(baseKind[4])
		if (magazines) kind.add(baseKind[5])
		year1 = Integer.parseInt(from)
		year2 = Integer.parseInt(to)
	}

	/**
	 * 	Returns an ImageIcon, or null if the path was invalid.
	 */
	ImageIcon createImageIcon(String path){
		new ImageIcon((URL) ClassLoader.getSystemResource(path))
	}

	Border createBorder(String text){
		BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(text),
				BorderFactory.createEmptyBorder(4, 4, 4, 4))
	}

	@TypeChecked(SKIP)
	def graphics() {
		def fc = new JFileChooser()

		new SwingBuilder().edt {
			//Handle open button action.
			openDialog = {
				if (fc.showOpenDialog(panel1) == JFileChooser.APPROVE_OPTION)
					openFile.text = fc.selectedFile.absolutePath
				else
					log.append('Open command cancelled by user.\n')
				log.caretPosition = log.document.length
			}
			//Handle Save button action
			saveDialog = {
				if (fc.showSaveDialog(panel1) == JFileChooser.APPROVE_OPTION)
					saveFile.text = fc.selectedFile.absolutePath
				else
					log.append 'Saved command cancelled by user.\n'
				log.caretPosition = log.document.length
			}
			//Do convertion.
			start = {
				log.append 'Processing ...\n'
				//  Test if file exists, if it does, ask user permission to overwrite it.
				if (!new File(saveFile.text).exists() ||
				(JOptionPane.showConfirmDialog(null, "File ${saveFile.text} Already Exists, overwrite it?", 'File Exists', JOptionPane.YES_NO_OPTION)
				==JOptionPane.YES_OPTION)){
					try {
						setPreferences(journals.isSelected(), conferences.isSelected(), books.isSelected(),
								chapters.isSelected(), newspapers.isSelected(), magazines.isSelected(),
								from.text, to.text)

						path(openFile.text, saveFile.text)
						log.append("Done !!!\nCheck results at:\n${saveFile.text}")
					}
					catch (e) {log.append('Error:\n' + e.toString() + '\n')}
				}
			}

			frame(title:'Lattes to EndNote Converter', defaultCloseOperation:JFrame.EXIT_ON_CLOSE, pack:true, show:true){
				borderLayout()
				vbox(border:createBorder('Lattes XML to EndNote Tab Format Reference Extractor and Converter')){
					panel1 = panel{
						button(text:'Open Lattes XML File...', icon:createImageIcon('images/Open16.gif'),
						actionPerformed:openDialog)
						openFile = textField(columns:20)
					}
					panel{
						button(text:'Save EndNote File...', actionPerformed: saveDialog, icon:createImageIcon('images/Save16.gif'))
						saveFile = textField(columns:20)
					}
					panel(border:createBorder('Choose publications to include:')){
						journals = checkBox(text:'Journals', selected:true)
						conferences = checkBox(text:'Conferences', selected:true)
						books = checkBox(text:'Books', selected:true)
						chapters = checkBox(text:'Chapters', selected:true)
						newspapers = checkBox(text:'Newspapers', selected:true)
						magazines = checkBox(text:'Magazines', selected:true)
					}
					panel(border:createBorder('Choose period in years:')){
						label('Beginning in: ')
						from = textField(text:'1100', columns:4)
						label('Ending in: ')
						to = textField(text:'3000', columns:4)
					}
					button(text:'Start', actionPerformed:start)
					log = textArea(rows:5, columns:15, editable:false, margin:new Insets(5,5,5,5), autoscrolls:true)
				}
			}
		}
	}

	@TypeChecked(SKIP)
	static main(args) {
		def cli = new CliBuilder(usage: 'command lattes_xml_file endnote_file')
		cli.with{
			h 'Show usage information'
			nojournals 'No journal papers'
			noconferences 'No conference papers'
			nobooks 'No books'
			nochapters 'No book chapters'
			nonewspapers 'No newspapers articles'
			nomagazines 'No magazine articles'
			from args: 1, argName: 'year beginning', 'From year'
			to args: 1, argName: 'year ending', 'To year'
		}

		def options = cli.parse(args)
		// Show usage text when -h or --help option is used.
		if (options.h){ // || options.arguments().size() != 2) {
			cli.usage()
			return
		}
		def lattes = new Lattes2Endnote(year1:1900, year2:3000)
		if(!options || options.arguments().size()!=2) {
			lattes.graphics()
			return
		}

		lattes.setPreferences (
				!options.nojournals,
				!options.noconferences,
				!options.nobooks,
				!options.nochapters,
				!options.nonewspapers,
				!options.nomagazines,
				(options.from)?options.from:lattes.year1,
				(options.to)?options.to:lattes.year2)

		lattes.path(options.arguments()[0], options.arguments()[1])
	}
}
