/**
 * Copyright (c) 2013, Persado Intellectual Property Limited. All rights
 * reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * * Neither the name of the Persado Intellectual Property Limited nor the names
 * of its contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * 
 */
package com.persado.oss.quality.stevia.selenium.core.controllers.webdriverapi;

import java.util.List;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.FindsByCssSelector;
import org.openqa.selenium.internal.FindsByXPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.persado.oss.quality.stevia.selenium.core.SteviaContext;
import com.persado.oss.quality.stevia.selenium.core.controllers.WebDriverWebController;

public abstract class ByExtended extends By {

	private static final Logger LOG = LoggerFactory.getLogger(ByExtended.class);
	
	/**
	 * Finds elements via the driver's underlying W3 Selector engine. If the
	 * browser does not implement the Selector API, a best effort is made to
	 * emulate the API. In this case, we strive for at least CSS2 support, but
	 * offer no guarantees.
	 */
	public static By cssSelector(final String selector) {
		if (selector == null)
			throw new IllegalArgumentException(
					"Cannot find elements when the selector is null");

		return new ByCssSelectorExtended(selector);

	}
	
	 /**
	   * @param xpathExpression The xpath to use
	   * @return a By which locates elements via XPath
	   */
	  public static By xpath(final String xpathExpression) {
	    if (xpathExpression == null)
	      throw new IllegalArgumentException(
	          "Cannot find elements when the XPath expression is null.");

	    return new ByXPathExtended(xpathExpression);
	  }
	

	public static class ByCssSelectorExtended extends ByCssSelector {

		private String ownSelector;

		public ByCssSelectorExtended(String selector) {
			super(selector);
			ownSelector = selector;
		}

		@Override
		public WebElement findElement(SearchContext context) {
			try {
				if (context instanceof FindsByCssSelector) {
					return ((FindsByCssSelector) context)
							.findElementByCssSelector(ownSelector);
				}
			} catch (InvalidElementStateException e) {
				return findElementBySizzleCss(ownSelector);
			} catch (WebDriverException e) {
				if (e.getMessage().startsWith(
						"An invalid or illegal string was specified")) {
					return findElementBySizzleCss(ownSelector);
				}
				throw e;
			}
			throw new WebDriverException("Driver does not support finding an element by selector: "	+ ownSelector);
		}

		@Override
		public List<WebElement> findElements(SearchContext context) {
			try {
				if (context instanceof FindsByCssSelector) {
					return ((FindsByCssSelector) context)
							.findElementsByCssSelector(ownSelector);
				}
			} catch (InvalidElementStateException e) {
				return findElementsBySizzleCss(ownSelector);
			} catch (WebDriverException e) {
				if (e.getMessage().startsWith(
						"An invalid or illegal string was specified")) {
					return findElementsBySizzleCss(ownSelector);
				}
				throw e;
			}
			throw new WebDriverException("Driver does not support finding an element by selector: "	+ ownSelector);
		}

		@Override
		public String toString() {
			return "ByExtended.selector: " + ownSelector;
		}


		 /********************************* SIZZLE SUPPORT CODE**************************************/
	

		/**
		 * Find element by sizzle css.
		 * 
		 * @param cssLocator
		 *            the cssLocator
		 * @return the web element
		 */
		@SuppressWarnings("unchecked")
		public WebElement findElementBySizzleCss(String cssLocator) {
			injectSizzleIfNeeded();
			String javascriptExpression = createSizzleSelectorExpression(cssLocator);
			List<WebElement> elements = (List<WebElement>) ((JavascriptExecutor) getDriver())
					.executeScript(javascriptExpression);
			if (elements.size() > 0)
				return (WebElement) elements.get(0);
			return null;
		}

		private WebDriver getDriver() {
			WebDriverWebController controller = ((WebDriverWebController) SteviaContext.getWebController());
			return controller.getDriver();
		}

		/**
		 * Find elements by sizzle css.
		 * 
		 * @param cssLocator
		 *            the cssLocator
		 * @return the list of the web elements that match this locator
		 */
		@SuppressWarnings("unchecked")
		public List<WebElement> findElementsBySizzleCss(String cssLocator) {
			injectSizzleIfNeeded();
			String javascriptExpression = createSizzleSelectorExpression(cssLocator);
			return (List<WebElement>) ((JavascriptExecutor) getDriver()).executeScript(javascriptExpression);
		}

		/**
		 * Creates the sizzle selector expression.
		 * 
		 * @param cssLocator
		 *            the cssLocator
		 * @return string that represents the sizzle selector expression.
		 */
		private String createSizzleSelectorExpression(String cssLocator) {
			return "return Sizzle(\"" + cssLocator + "\")";
		}

		/**
		 * Inject sizzle if needed.
		 */
		private void injectSizzleIfNeeded() {
			if (!sizzleLoaded())
				injectSizzle();
		}

		/**
		 * Check if the Sizzle library is loaded.
		 * 
		 * @return the true if Sizzle is loaded in the web page 
		 */
		public Boolean sizzleLoaded() {
			Boolean loaded;
			try {
				loaded = (Boolean) ((JavascriptExecutor) getDriver())
						.executeScript("return Sizzle()!=null");
			} catch (WebDriverException e) {
				loaded = false;
			}
			return loaded;
		}

		/**
		 * Inject sizzle 1.8.2
		 */
		public void injectSizzle() {
			((JavascriptExecutor) getDriver())
					.executeScript(" var headID = document.getElementsByTagName(\"head\")[0];"
							+ "var newScript = document.createElement('script');"
							+ "newScript.type = 'text/javascript';"
							+ "newScript.src = 'https://raw.github.com/jquery/sizzle/1.8.2/sizzle.js';"
							+ "headID.appendChild(newScript);");
		}
		/**
		 * ******************** SIZZLE SUPPORT CODE
		 */

	}

	public static class ByXPathExtended extends ByXPath {
		private final String ownXpathExpression;

		public ByXPathExtended(String xpathExpression) {
			super(xpathExpression);
			ownXpathExpression = xpathExpression;
		}

		@Override
		public List<WebElement> findElements(SearchContext context) {
			long t0 = System.currentTimeMillis();
			try {
				return ((FindsByXPath) context)
					.findElementsByXPath(ownXpathExpression);
			} finally {
				long l = System.currentTimeMillis()-t0;
				if (l > 100) {
					LOG.warn("SLOW findElements() = {}ms. Slow selector : {} ", l,  ownXpathExpression);
				}
			}
		}

		@Override
		public WebElement findElement(SearchContext context) {
			long t0 = System.currentTimeMillis();
			try {
				int indexOf = ownXpathExpression.indexOf("//", 3);
				if (indexOf > -1) { // we found an // inside the selector
					String[] splitSelectors = ownXpathExpression.substring(2).split(Pattern.quote("//"));
					
					WebElement parent = ((FindsByXPath) context).findElementByXPath("//"+splitSelectors[0]);
					for (int i = 1; i < splitSelectors.length; i++) {
						if (parent == null) {
							throw new WebDriverException("Failed to match the parent selector : "+splitSelectors[i-1]);
						}
						WebElement found = parent.findElement(By.xpath(".//"+splitSelectors[i]));
						if (found != null) {
							parent = found;
						} else {
							throw new WebDriverException("Failed to match the selector : "+splitSelectors[i]+" within "+ownXpathExpression);
						}
					}
					
					// by here, we should have the parent WebElement to contain what we want.
					//LOG.info("Found compount selector : "+parent.toString());
					return parent;
				}
				// simple case: one selector
				return ((FindsByXPath) context).findElementByXPath(ownXpathExpression);
			} finally {
				long l = System.currentTimeMillis()-t0;
				if (l > 100) {
					LOG.warn("SLOW findElement() = {}ms. Slow selector : {} ", l,  ownXpathExpression);
				}
			}
		}

		@Override
		public String toString() {
			return "ByExtended.xpath: " + ownXpathExpression;
		}
	}
}